package com.cedarsoftware.util;

import java.util.*;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.UUID;

/**
 * ConcurrentNavigableMapNullSafe is a thread-safe implementation of ConcurrentNavigableMap
 * that allows null keys and null values by using a unique String sentinel for null keys.
 * From an ordering perspective, null keys are considered last.  This is honored with the
 * ascending and descending views, where ascending view places them last, and descending view
 * place a null key first.
 *
 * @param <K> The type of keys maintained by this map
 * @param <V> The type of mapped values
 *           
 * @author John DeRegnaucourt (jdereg@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
public class ConcurrentNavigableMapNullSafe<K, V> extends AbstractConcurrentNullSafeMap<K, V>
        implements ConcurrentNavigableMap<K, V> {

    private final Comparator<? super K> originalComparator;
    private static final String NULL_KEY_SENTINEL = "null_" + UUID.randomUUID();

    /**
     * Constructs a new, empty ConcurrentNavigableMapNullSafe with natural ordering of its keys.
     * All keys inserted must implement the Comparable interface.
     */
    public ConcurrentNavigableMapNullSafe() {
        this(null);
    }

    /**
     * Constructs a new, empty ConcurrentNavigableMapNullSafe with the specified comparator.
     *
     * @param comparator the comparator that will be used to order this map. If null, the natural
     *                   ordering of the keys will be used.
     */
    public ConcurrentNavigableMapNullSafe(Comparator<? super K> comparator) {
        this(new ConcurrentSkipListMap<>(wrapComparator(comparator)), comparator);
    }

    /**
     * Private constructor that accepts an internal map and the original comparator.
     *
     * @param internalMap       the internal map to wrap
     * @param originalComparator the original comparator provided by the user
     */
    private ConcurrentNavigableMapNullSafe(ConcurrentNavigableMap<Object, Object> internalMap, Comparator<? super K> originalComparator) {
        super(internalMap);
        this.originalComparator = originalComparator;
    }

    /**
     * Static method to wrap the user-provided comparator to handle sentinel keys and mixed key types.
     *
     * @param comparator the user-provided comparator
     * @return a comparator that handles sentinel keys and mixed key types
     */
    @SuppressWarnings("unchecked")
    private static <K> Comparator<Object> wrapComparator(Comparator<? super K> comparator) {
        return (o1, o2) -> {
            // Handle the sentinel value for null keys
            boolean o1IsNullSentinel = NULL_KEY_SENTINEL.equals(o1);
            boolean o2IsNullSentinel = NULL_KEY_SENTINEL.equals(o2);

            if (o1IsNullSentinel && o2IsNullSentinel) {
                return 0;
            }
            if (o1IsNullSentinel) {
                return 1; // Null keys are considered greater than any other keys
            }
            if (o2IsNullSentinel) {
                return -1;
            }

            // Handle actual nulls (should not occur)
            if (o1 == null && o2 == null) {
                return 0;
            }
            if (o1 == null) {
                return -1;
            }
            if (o2 == null) {
                return 1;
            }

            // Use the provided comparator if available
            if (comparator != null) {
                return comparator.compare((K) o1, (K) o2);
            }

            // If keys are of the same class and Comparable, compare them
            if (o1.getClass() == o2.getClass() && o1 instanceof Comparable) {
                return ((Comparable<Object>) o1).compareTo(o2);
            }

            // Compare class names to provide ordering between different types
            String className1 = o1.getClass().getName();
            String className2 = o2.getClass().getName();
            int classComparison = className1.compareTo(className2);

            if (classComparison != 0) {
                return classComparison;
            }

            // If class names are the same but classes are different (rare), compare identity hash codes
            return Integer.compare(System.identityHashCode(o1.getClass()), System.identityHashCode(o2.getClass()));
        };
    }

    @Override
    protected Object maskNullKey(K key) {
        if (key == null) {
            return NULL_KEY_SENTINEL;
        }
        return key;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected K unmaskNullKey(Object maskedKey) {
        if (NULL_KEY_SENTINEL.equals(maskedKey)) {
            return null;
        }
        return (K) maskedKey;
    }

    @Override
    public Comparator<? super K> comparator() {
        return originalComparator;
    }

    // Implement navigational methods

    @Override
    public ConcurrentNavigableMap<K, V> subMap(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        ConcurrentNavigableMap<Object, Object> subInternal = ((ConcurrentNavigableMap<Object, Object>) internalMap).subMap(
                maskNullKey(fromKey), fromInclusive,
                maskNullKey(toKey), toInclusive
        );
        return new ConcurrentNavigableMapNullSafe<>(subInternal, this.originalComparator);
    }

    @Override
    public ConcurrentNavigableMap<K, V> headMap(K toKey, boolean inclusive) {
        ConcurrentNavigableMap<Object, Object> headInternal = ((ConcurrentNavigableMap<Object, Object>) internalMap).headMap(
                maskNullKey(toKey), inclusive
        );
        return new ConcurrentNavigableMapNullSafe<>(headInternal, this.originalComparator);
    }

    @Override
    public ConcurrentNavigableMap<K, V> tailMap(K fromKey, boolean inclusive) {
        ConcurrentNavigableMap<Object, Object> tailInternal = ((ConcurrentNavigableMap<Object, Object>) internalMap).tailMap(
                maskNullKey(fromKey), inclusive
        );
        return new ConcurrentNavigableMapNullSafe<>(tailInternal, this.originalComparator);
    }

    @Override
    public ConcurrentNavigableMap<K, V> subMap(K fromKey, K toKey) {
        return subMap(fromKey, true, toKey, false);
    }

    @Override
    public ConcurrentNavigableMap<K, V> headMap(K toKey) {
        return headMap(toKey, false);
    }

    @Override
    public ConcurrentNavigableMap<K, V> tailMap(K fromKey) {
        return tailMap(fromKey, true);
    }

    @Override
    public Entry<K, V> lowerEntry(K key) {
        Entry<Object, Object> entry = ((ConcurrentSkipListMap<Object, Object>) internalMap).lowerEntry(maskNullKey(key));
        return wrapEntry(entry);
    }

    @Override
    public K lowerKey(K key) {
        return unmaskNullKey(((ConcurrentSkipListMap<Object, Object>) internalMap).lowerKey(maskNullKey(key)));
    }

    @Override
    public Entry<K, V> floorEntry(K key) {
        Entry<Object, Object> entry = ((ConcurrentSkipListMap<Object, Object>) internalMap).floorEntry(maskNullKey(key));
        return wrapEntry(entry);
    }

    @Override
    public K floorKey(K key) {
        return unmaskNullKey(((ConcurrentSkipListMap<Object, Object>) internalMap).floorKey(maskNullKey(key)));
    }

    @Override
    public Entry<K, V> ceilingEntry(K key) {
        Entry<Object, Object> entry = ((ConcurrentSkipListMap<Object, Object>) internalMap).ceilingEntry(maskNullKey(key));
        return wrapEntry(entry);
    }

    @Override
    public K ceilingKey(K key) {
        return unmaskNullKey(((ConcurrentSkipListMap<Object, Object>) internalMap).ceilingKey(maskNullKey(key)));
    }

    @Override
    public Entry<K, V> higherEntry(K key) {
        Entry<Object, Object> entry = ((ConcurrentSkipListMap<Object, Object>) internalMap).higherEntry(maskNullKey(key));
        return wrapEntry(entry);
    }

    @Override
    public K higherKey(K key) {
        return unmaskNullKey(((ConcurrentSkipListMap<Object, Object>) internalMap).higherKey(maskNullKey(key)));
    }

    @Override
    public Entry<K, V> firstEntry() {
        Entry<Object, Object> entry = ((ConcurrentSkipListMap<Object, Object>) internalMap).firstEntry();
        return wrapEntry(entry);
    }

    @Override
    public Entry<K, V> lastEntry() {
        Entry<Object, Object> entry = ((ConcurrentSkipListMap<Object, Object>) internalMap).lastEntry();
        return wrapEntry(entry);
    }

    @Override
    public Entry<K, V> pollFirstEntry() {
        Entry<Object, Object> entry = ((ConcurrentSkipListMap<Object, Object>) internalMap).pollFirstEntry();
        return wrapEntry(entry);
    }

    @Override
    public Entry<K, V> pollLastEntry() {
        Entry<Object, Object> entry = ((ConcurrentSkipListMap<Object, Object>) internalMap).pollLastEntry();
        return wrapEntry(entry);
    }

    @Override
    public K firstKey() {
        return unmaskNullKey(((ConcurrentSkipListMap<Object, Object>) internalMap).firstKey());
    }

    @Override
    public K lastKey() {
        return unmaskNullKey(((ConcurrentSkipListMap<Object, Object>) internalMap).lastKey());
    }

    @Override
    public NavigableSet<K> navigableKeySet() {
        return keySet();
    }

    @Override
    public NavigableSet<K> descendingKeySet() {
        return descendingMap().navigableKeySet();
    }

    @Override
    public ConcurrentNavigableMap<K, V> descendingMap() {
        ConcurrentNavigableMap<Object, Object> descInternal = ((ConcurrentNavigableMap<Object, Object>) internalMap).descendingMap();
        return new ConcurrentNavigableMapNullSafe<>(descInternal, this.originalComparator);
    }

    @Override
    public NavigableSet<K> keySet() {
        Set<Object> internalKeys = internalMap.keySet();
        return new KeyNavigableSet(internalKeys);
    }

    /**
     * Inner class implementing NavigableSet<K> for the keySet().
     */
    private class KeyNavigableSet extends AbstractSet<K> implements NavigableSet<K> {
        private final Set<Object> internalKeys;

        KeyNavigableSet(Set<Object> internalKeys) {
            this.internalKeys = internalKeys;
        }

        @Override
        public Iterator<K> iterator() {
            Iterator<Object> it = internalKeys.iterator();
            return new Iterator<K>() {
                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public K next() {
                    return unmaskNullKey(it.next());
                }

                @Override
                public void remove() {
                    it.remove();
                }
            };
        }

        @Override
        public int size() {
            return internalKeys.size();
        }

        @Override
        public boolean contains(Object o) {
            return internalMap.containsKey(maskNullKey((K) o));
        }

        @Override
        public boolean remove(Object o) {
            return internalMap.remove(maskNullKey((K) o)) != null;
        }

        @Override
        public void clear() {
            internalMap.clear();
        }

        @Override
        public K lower(K k) {
            return unmaskNullKey(((ConcurrentSkipListMap<Object, Object>) internalMap).lowerKey(maskNullKey(k)));
        }

        @Override
        public K floor(K k) {
            return unmaskNullKey(((ConcurrentSkipListMap<Object, Object>) internalMap).floorKey(maskNullKey(k)));
        }

        @Override
        public K ceiling(K k) {
            return unmaskNullKey(((ConcurrentSkipListMap<Object, Object>) internalMap).ceilingKey(maskNullKey(k)));
        }

        @Override
        public K higher(K k) {
            return unmaskNullKey(((ConcurrentSkipListMap<Object, Object>) internalMap).higherKey(maskNullKey(k)));
        }

        @Override
        public K pollFirst() {
            Entry<Object, Object> entry = ((ConcurrentSkipListMap<Object, Object>) internalMap).pollFirstEntry();
            return (entry == null) ? null : unmaskNullKey(entry.getKey());
        }

        @Override
        public K pollLast() {
            Entry<Object, Object> entry = ((ConcurrentSkipListMap<Object, Object>) internalMap).pollLastEntry();
            return (entry == null) ? null : unmaskNullKey(entry.getKey());
        }

        @Override
        public Comparator<? super K> comparator() {
            return ConcurrentNavigableMapNullSafe.this.comparator();
        }

        @Override
        public K first() {
            return unmaskNullKey(((ConcurrentSkipListMap<Object, Object>) internalMap).firstKey());
        }

        @Override
        public K last() {
            return unmaskNullKey(((ConcurrentSkipListMap<Object, Object>) internalMap).lastKey());
        }

        @Override
        public NavigableSet<K> descendingSet() {
            return ConcurrentNavigableMapNullSafe.this.descendingKeySet();
        }

        @Override
        public Iterator<K> descendingIterator() {
            Iterator<Object> it = ((ConcurrentSkipListMap<Object, Object>) internalMap).descendingKeySet().iterator();
            return new Iterator<K>() {
                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public K next() {
                    return unmaskNullKey(it.next());
                }

                @Override
                public void remove() {
                    it.remove();
                }
            };
        }

        @Override
        public NavigableSet<K> subSet(K fromElement, boolean fromInclusive, K toElement, boolean toInclusive) {
            ConcurrentNavigableMap<K, V> subMap = ConcurrentNavigableMapNullSafe.this.subMap(fromElement, fromInclusive, toElement, toInclusive);
            return subMap.navigableKeySet();
        }

        @Override
        public NavigableSet<K> headSet(K toElement, boolean inclusive) {
            ConcurrentNavigableMap<K, V> headMap = ConcurrentNavigableMapNullSafe.this.headMap(toElement, inclusive);
            return headMap.navigableKeySet();
        }

        @Override
        public NavigableSet<K> tailSet(K fromElement, boolean inclusive) {
            ConcurrentNavigableMap<K, V> tailMap = ConcurrentNavigableMapNullSafe.this.tailMap(fromElement, inclusive);
            return tailMap.navigableKeySet();
        }

        @Override
        public SortedSet<K> subSet(K fromElement, K toElement) {
            return subSet(fromElement, true, toElement, false);
        }

        @Override
        public SortedSet<K> headSet(K toElement) {
            return headSet(toElement, false);
        }

        @Override
        public SortedSet<K> tailSet(K fromElement) {
            return tailSet(fromElement, true);
        }
    }

    /**
     * Wraps an internal entry to expose it as an Entry<K, V> with unmasked keys and values.
     *
     * @param internalEntry the internal map entry
     * @return the wrapped entry, or null if the internal entry is null
     */
    private Entry<K, V> wrapEntry(Entry<Object, Object> internalEntry) {
        if (internalEntry == null) return null;
        return new Entry<K, V>() {
            @Override
            public K getKey() {
                return unmaskNullKey(internalEntry.getKey());
            }

            @Override
            public V getValue() {
                return unmaskNullValue(internalEntry.getValue());
            }

            @Override
            public V setValue(V value) {
                Object oldValue = internalEntry.setValue(maskNullValue(value));
                return unmaskNullValue(oldValue);
            }

            @Override
            public boolean equals(Object o) {
                if (!(o instanceof Entry)) return false;
                Entry<?, ?> e = (Entry<?, ?>) o;
                return Objects.equals(getKey(), e.getKey()) &&
                        Objects.equals(getValue(), e.getValue());
            }

            @Override
            public int hashCode() {
                return Objects.hashCode(getKey()) ^ Objects.hashCode(getValue());
            }

            @Override
            public String toString() {
                return getKey() + "=" + getValue();
            }
        };
    }
}
