const {test} = require('node:test');
const assert = require('node:assert/strict');
const {hasCapacity, priceOrder,canTransition,foodTransitions,reportTransitions} = require('../policy');
test('capacity blocks last-seat overflow and closed registration',()=>{
 assert.equal(hasCapacity({capacity:1,registeredCount:1,registrationEnabled:true}),false);
 assert.equal(hasCapacity({capacity:1,registeredCount:0,registrationEnabled:true}),true);
 assert.equal(hasCapacity({capacity:0,registeredCount:100,registrationEnabled:true}),true);
 assert.equal(hasCapacity({capacity:10,registrationEnabled:false}),false);
 assert.equal(hasCapacity({capacity:-1,registrationEnabled:true}),false);
 assert.equal(hasCapacity({capacity:1,registeredCount:-1,registrationEnabled:true}),false);
});
test('order prices come from menu and reject unavailable/duplicate/invalid quantity',()=>{
 const menu=new Map([['a',{name:'Tea',pricePaise:1500,available:true}]]);
 assert.deepEqual(priceOrder(menu,[{menuItemId:'a',quantity:2,pricePaise:1}]),[{menuItemId:'a',name:'Tea',quantity:2,pricePaise:1500}]);
 for(const quantity of [0,-1,21,1.5]) assert.throws(()=>priceOrder(menu,[{menuItemId:'a',quantity}]));
 assert.throws(()=>priceOrder(menu,[{menuItemId:'a',quantity:1},{menuItemId:'a',quantity:2}]));
 assert.throws(()=>priceOrder(menu,[{menuItemId:'missing',quantity:1}]));
});
test('order and report terminal states cannot be reopened',()=>{
 assert.equal(canTransition(foodTransitions,'PLACED','READY'),false);
 assert.equal(canTransition(foodTransitions,'READY','COMPLETED'),true);
 assert.equal(canTransition(foodTransitions,'COMPLETED','PLACED'),false);
 assert.equal(canTransition(reportTransitions,'SUBMITTED','RESOLVED'),false);
 assert.equal(canTransition(reportTransitions,'IN_PROGRESS','RESOLVED'),true);
 assert.equal(canTransition(reportTransitions,'RESOLVED','IN_PROGRESS'),false);
});
