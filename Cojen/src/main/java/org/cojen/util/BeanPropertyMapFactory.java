/*
 *  Copyright 2007 Brian S O'Neill
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.cojen.util;

import java.lang.ref.SoftReference;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Provides a simple and efficient means of reading and writing bean properties
 * via a map.
 *
 * @author Brian S O'Neill
 * @see BeanPropertyAccessor
 * @since 2.1
 */
public abstract class BeanPropertyMapFactory<B> {
    private static final Map<Class, SoftReference<BeanPropertyMapFactory>> cFactories =
        new WeakIdentityMap<Class, SoftReference<BeanPropertyMapFactory>>();

    /**
     * Returns a new or cached BeanPropertyMapFactory for the given class.
     */
    public static <B> BeanPropertyMapFactory<B> forClass(Class<B> clazz) {
        synchronized (cFactories) {
            BeanPropertyMapFactory factory;
            SoftReference<BeanPropertyMapFactory> ref = cFactories.get(clazz);
            if (ref != null) {
                factory = ref.get();
                if (factory != null) {
                    return factory;
                }
            }

	    Map<String, BeanProperty> properties = BeanIntrospector.getAllProperties(clazz);

	    if (properties.size() == 0) {
		factory = Empty.INSTANCE;
	    } else {
		factory = new Standard<B>(BeanPropertyAccessor.forClass(clazz), properties);
	    }

            cFactories.put(clazz, new SoftReference<BeanPropertyMapFactory>(factory));
            return factory;
        }
    }

    /**
     * Returns a fixed-size map backed by the given bean. Map remove operations
     * are unsupported, as are put operations on non-existent properties.

     * @throws IllegalArgumentException if bean is null
     */
    public static Map<String, Object> asMap(Object bean) {
	if (bean == null) {
	    throw new IllegalArgumentException();
	}
	BeanPropertyMapFactory factory = forClass(bean.getClass());
	return factory.createMap(bean);
    }

    /**
     * Returns a fixed-size map backed by the given bean. Map remove operations
     * are unsupported, as are put operations on non-existent properties.
     *
     * @throws IllegalArgumentException if bean is null
     */
    public abstract Map<String, Object> createMap(B bean);

    private static class Empty extends BeanPropertyMapFactory {
	static final Empty INSTANCE = new Empty();

	private Empty() {
	}

	public Map<String, Object> createMap(Object bean) {
	    if (bean == null) {
		throw new IllegalArgumentException();
	    }
	    return Collections.emptyMap();
	}
    }

    private static class Standard<B> extends BeanPropertyMapFactory<B> {
	final BeanPropertyAccessor mAccessor;
	final Set<String> mPropertyNames;

	public Standard(BeanPropertyAccessor<B> accessor, Map<String, BeanProperty> properties) {
	    mAccessor = accessor;

	    // Only reveal readable properties.
	    Set<String> propertyNames = new HashSet<String>(properties.size());
	    for (BeanProperty property : properties.values()) {
		if (property.getReadMethod() != null) {
		    propertyNames.add(property.getName());
		}
	    }

	    mPropertyNames = Collections.unmodifiableSet(propertyNames);
	}

	public Map<String, Object> createMap(B bean) {
	    if (bean == null) {
		throw new IllegalArgumentException();
	    }
	    return new BeanMap<B>(bean, mAccessor, mPropertyNames);
	}
    }

    private static class BeanMap<B> extends AbstractMap<String, Object> {
	final B mBean;
	final BeanPropertyAccessor mAccessor;
	final Set<String> mPropertyNames;

	BeanMap(B bean, BeanPropertyAccessor<B> accessor, Set<String> propertyNames) {
	    mBean = bean;
	    mAccessor = accessor;
	    mPropertyNames = propertyNames;
	}

	@Override
	public int size() {
	    return mPropertyNames.size();
	}

	@Override
	public boolean isEmpty() {
	    return false;
	}

	@Override
	public boolean containsKey(Object key) {
	    return mAccessor.hasReadableProperty((String) key);
	}

	@Override
	public boolean containsValue(Object value) {
	    return mAccessor.hasPropertyValue(mBean, value);
	}

	@Override
	public Object get(Object key) {
	    return mAccessor.getPropertyValue(mBean, (String) key);
	}

	@Override
	public Object put(String key, Object value) {
	    Object old = mAccessor.getPropertyValue(mBean, key);
	    mAccessor.setPropertyValue(mBean, key, value);
	    return old;
	}

	@Override
	public Object remove(Object key) {
	    throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
	    throw new UnsupportedOperationException();
	}

	@Override
	public Set<String> keySet() {
	    return mPropertyNames;
	}

	@Override
	public Collection<Object> values() {
	    return new AbstractCollection<Object>() {
		@Override
		public Iterator<Object> iterator() {
		    return new Iterator<Object>() {
			private final Iterator<String> mPropIterator = keySet().iterator();

			public boolean hasNext() {
			    return mPropIterator.hasNext();
			}

			public Object next() {
			    return get(mPropIterator.next());
			}

			public void remove() {
			    throw new UnsupportedOperationException();
			}
		    };
		}
		
		@Override
		public int size() {
		    return BeanMap.this.size();
		}

		@Override
		public boolean isEmpty() {
		    return false;
		}

		@Override
		public boolean contains(Object v) {
		    return containsValue(v);
		}

		@Override
		public boolean remove(Object e) {
		    throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
		    throw new UnsupportedOperationException();
		}
	    };
	}

	@Override
	public Set<Map.Entry<String, Object>> entrySet() {
	    return new AbstractSet<Map.Entry<String, Object>>() {
		@Override
		public Iterator<Map.Entry<String, Object>> iterator() {
		    return new Iterator<Map.Entry<String, Object>>() {
			private final Iterator<String> mPropIterator = keySet().iterator();

			public boolean hasNext() {
			    return mPropIterator.hasNext();
			}

			public Map.Entry<String, Object> next() {
			    final String property = mPropIterator.next();
			    final Object value = get(property);

			    return new Map.Entry<String, Object>() {
				Object mutableValue = value;

				public String getKey() {
				    return property;
				}

				public Object getValue() {
				    return mutableValue;
				}

				public Object setValue(Object value) {
				    Object old = BeanMap.this.put(property, value);
				    mutableValue = value;
				    return old;
				}

				@Override
				public boolean equals(Object obj) {
				    if (this == obj) {
					return true;
				    }

				    if (obj instanceof Map.Entry) {
					Map.Entry other = (Map.Entry) obj;

					return
					    (this.getKey() == null ?
					     other.getKey() == null
					     : this.getKey().equals(other.getKey()))
					    &&
					    (this.getValue() == null ?
					     other.getValue() == null
					     : this.getValue().equals(other.getValue()));
				    }

				    return false;
				}

				@Override
				public int hashCode() {
				    return (getKey() == null ? 0 : getKey().hashCode()) ^
					(getValue() == null ? 0 : getValue().hashCode());
				}

				@Override
				public String toString() {
				    return property + "=" + mutableValue;
				}
			    };
			}

			public void remove() {
			    throw new UnsupportedOperationException();
			}
		    };
		}

		@Override
		public int size() {
		    return BeanMap.this.size();
		}

		@Override
		public boolean isEmpty() {
		    return false;
		}

		@Override
		public boolean contains(Object e) {
		    Map.Entry<String, Object> entry = (Map.Entry<String, Object>) e;
		    String key = entry.getKey();
		    if (BeanMap.this.containsKey(key)) {
			Object value = BeanMap.this.get(key);
			return value == null ? entry.getValue() == null
			    : value.equals(entry.getValue());
		    }
		    return false;
		}

		@Override
		public boolean add(Map.Entry<String, Object> e) {
		    BeanMap.this.put(e.getKey(), e.getValue());
		    return true;
		}

		@Override
		public boolean remove(Object e) {
		    throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
		    throw new UnsupportedOperationException();
		}
	    };
	}
    }
}
