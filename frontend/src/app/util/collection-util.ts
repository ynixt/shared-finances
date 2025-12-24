export function groupArrayBy<TValue, TKey>(list: Array<TValue>, keyGetter: (item: TValue) => TKey): Map<TKey, TValue> {
  const map = new Map<TKey, TValue>();
  list.forEach(item => {
    const key = keyGetter(item);
    map.set(key, item);
  });
  return map;
}
