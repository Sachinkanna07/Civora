const foodTransitions = {PLACED: ['ACCEPTED', 'CANCELLED'], ACCEPTED: ['PREPARING', 'CANCELLED'], PREPARING: ['READY'], READY: ['COMPLETED']};
const reportTransitions = {SUBMITTED: ['ASSIGNED', 'REJECTED'], ASSIGNED: ['IN_PROGRESS', 'REJECTED'], IN_PROGRESS: ['RESOLVED', 'REJECTED']};
function canTransition(table, from, to) { return (table[from] || []).includes(to); }
function hasCapacity(event) { return event.registrationEnabled === true && Number.isInteger(event.capacity) && event.capacity >= 0 && Number.isInteger(event.registeredCount) && event.registeredCount >= 0 && (event.capacity === 0 || event.registeredCount < event.capacity); }
function priceOrder(menu, quantities) {
  if (!Array.isArray(quantities) || quantities.length < 1 || quantities.length > 30) throw Error('Invalid cart');
  const seen = new Set();
  return quantities.map(({menuItemId, quantity}) => {
    if (seen.has(menuItemId)) throw Error('Duplicate item'); seen.add(menuItemId);
    const item = menu.get(menuItemId);
    if (!item || !item.available || !Number.isInteger(quantity) || quantity < 1 || quantity > 20 || !Number.isSafeInteger(item.pricePaise) || item.pricePaise < 0 || item.pricePaise > 100000) throw Error('Invalid item');
    return {menuItemId, name: item.name, quantity, pricePaise: item.pricePaise};
  });
}
module.exports = {foodTransitions, reportTransitions, canTransition, hasCapacity, priceOrder};
