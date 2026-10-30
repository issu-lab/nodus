const sharp = require('sharp');
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const brand = path.join(root, 'assets/branding');
(async () => {
  await sharp(path.join(brand,'source/nodus-icon.svg')).resize(1024,1024).png().toFile(path.join(brand,'nodus-icon.png'));
  for (const theme of ['dark','light']) {
    const svg = path.join(brand,'source',theme === 'dark' ? 'nodus-logo.svg' : 'nodus-logo-light.svg');
    const transparent = path.join(brand,theme === 'dark' ? 'nodus-logo.png' : 'nodus-logo-light-transparent.png');
    await sharp(svg,{density:144}).resize(960,1330).png().toFile(transparent);
    await sharp(transparent).flatten({background:theme === 'dark' ? '#000000' : '#FFFFFF'}).png().toFile(path.join(brand,`nodus-logo-${theme}.png`));
    fs.copyFileSync(transparent,path.join(root,'app/src/main/res/drawable-nodpi',theme === 'dark' ? 'nodus_logo.png' : 'nodus_logo_light.png'));
  }
})().catch(error=>{console.error(error);process.exitCode=1;});
