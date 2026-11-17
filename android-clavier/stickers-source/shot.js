const { chromium } = require('/opt/npm-tools/node_modules/playwright');
(async()=>{const b=await chromium.launch();const p=await b.newPage({viewport:{width:512,height:512}});
await p.goto('file:///home/claude/stickers/stickers.html');await p.waitForTimeout(800);
const n=await p.evaluate(()=>document.querySelectorAll('.s').length);
for(let i=0;i<n;i++){await (await p.$('#s'+i)).screenshot({path:`/home/claude/stickers/s${String(i+1).padStart(2,'0')}.png`,omitBackground:true});}
console.log(n);await b.close();})();
