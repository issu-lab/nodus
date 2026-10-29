const fs = require('node:fs');
const path = require('node:path');
const sharp = require('sharp');
const root = path.resolve(__dirname, '..');
(async () => {
  for (const [stem, width, height] of [['nodus-banner',5120,1280],['nodus-social-preview',1280,640]]) {
    const source = path.join(__dirname, `${stem}.svg`);
    if (!fs.existsSync(source)) throw new Error('Run python3 source/build_graphics.py first.');
    await sharp(source, {density:72}).resize(width,height).flatten({background:'#FFFFFF'})
      .removeAlpha().png().toFile(path.join(root,'assets',`${stem}.png`));
  }
  console.log('Canonical PNGs rendered at 5120×1280 and 1280×640.');
})().catch(error=>{console.error(error);process.exitCode=1;});
