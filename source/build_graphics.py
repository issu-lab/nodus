#!/usr/bin/env python3
"""Build outlined SVG artwork from the frozen iSSU template and approved noduS mark.

Only this project's sources/assets are written. Shared branding masters are not modified.
"""
from pathlib import Path
from copy import deepcopy
import base64
import json
import xml.etree.ElementTree as ET
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
from fontTools.pens.svgPathPen import SVGPathPen

ROOT = Path(__file__).resolve().parents[1]
HERE = ROOT / 'source'
PURPLE, MAGENTA, GRAPHITE = '#D94DFF', '#F800FF', '#151923'
NS = '{http://www.w3.org/2000/svg}'
ET.register_namespace('', NS[1:-1])
FONTS = {
    'title': instantiateVariableFont(TTFont(HERE / 'fonts/Exo2-Variable.ttf'), {'wght': 700}),
    'subtitle': instantiateVariableFont(TTFont(HERE / 'fonts/Inter-Variable.ttf'), {'wght': 600}),
}
N_PATH = ET.parse(ROOT / 'assets/branding/source/nodus-icon.svg').getroot().find(NS+'path').get('d')
lockup = ET.parse(ROOT / 'assets/branding/source/nodus-logo.svg').getroot()
S_PATH = next(e.get('d') for e in lockup.findall(NS+'path') if 'translate(548 675)' in e.get('transform', ''))
ICONS = {e.get('id'): e for e in ET.parse(HERE/'functional-icons.svg').getroot()}


def element(parent, name, **attrs):
    return ET.SubElement(parent, NS+name, {k.replace('_','-'):str(v) for k,v in attrs.items()})


def outlined(parent, text, x, y, size, role='title', fill=GRAPHITE):
    font = FONTS[role]; glyphs = font.getGlyphSet(); cmap = font.getBestCmap()
    scale = size / font['head'].unitsPerEm
    group = element(parent, 'g', transform=f'translate({x} {y}) scale({scale} {-scale})', fill=fill)
    group.set('data-text', text); group.set('data-font', 'Exo 2 Bold 700' if role=='title' else 'Inter Semibold 600')
    group.set('data-size', str(size)); pos = 0
    for char in text:
        glyph = glyphs[cmap[ord(char)]]; pen = SVGPathPen(glyphs); glyph.draw(pen)
        if pen.getCommands(): element(group, 'path', d=pen.getCommands(), transform=f'translate({pos} 0)')
        pos += glyph.width
    return pos * scale


def project_title(parent, x, y, size, gap):
    # Match the approved tightened u/S spacing, scaled from the 85px logo proof.
    outlined(parent, 'nodu', x, y, size)
    sx = x + size * (203 / 85)
    element(parent, 'path', d=S_PATH, fill=MAGENTA,
            transform=f'translate({sx} {y-size*.7}) scale({size/1000}) translate(-334 -40)',
            id='nodus-title-s')
    launcher_x = sx + 736 * size / 1000 + gap
    width = outlined(parent, 'LAUNCHER', launcher_x, y, size)
    return launcher_x + width


def image(parent, filename, **attrs):
    uri = 'data:image/png;base64,' + base64.b64encode((HERE/filename).read_bytes()).decode()
    return element(parent, 'image', href=uri, **attrs)


def icon(parent, symbol_id, center_x, center_y, radius, color, canonical_banner=False):
    scale = radius / 43
    group = element(parent, 'g', id=symbol_id, color=color, fill='none',
                    transform=f'translate({center_x-50*scale} {center_y-50*scale}) scale({scale})',
                    stroke_linecap='round', stroke_linejoin='round')
    for child in ICONS[symbol_id]:
        node = deepcopy(child)
        if canonical_banner and 'stroke-width' in node.attrib: node.set('stroke-width', str(12/scale))
        group.append(node)


def arcs(parent, banner):
    group=element(parent, 'g', id='corner-arcs', fill='none', stroke=PURPLE,
                  stroke_width=140 if banner else 40, opacity='.18')
    paths=['M 4740 1280 A 380 380 0 0 1 5120 900', 'M 4910 1280 A 210 210 0 0 1 5120 1070'] if banner else [
        'M 1090 640 A 190 190 0 0 1 1280 450', 'M 1155 640 A 125 125 0 0 1 1280 515']
    for path in paths: element(group,'path',d=path)


metrics={}
for banner in [True,False]:
    w,h=(5120,1280) if banner else (1280,640)
    root=ET.Element(NS+'svg', {'width':str(w),'height':str(h),'viewBox':f'0 0 {w} {h}', 'role':'img'})
    element(root,'title').text='noduS launcher — iSSU Open Homelab'
    element(root,'desc').text='Minimal Android launcher with a notification hub. Official iSSU layout; approved noduS mark in magenta.'
    element(root,'rect',width=w,height=h,fill='#FFFFFF')
    if banner:
        image(root,'issu-banner-wordmark.png',x=0,y=400,width=850,height=520)
        element(root,'path',id='nodus-project-mark',d=N_PATH,fill=MAGENTA,transform='translate(837.75 280) scale(2.25)')
        title_end=project_title(root,1650,685,250,50)
        description='MINIMAL ANDROID LAUNCHER WITH A NOTIFICATION HUB'
        subtitle_width=outlined(root,description,1650,835,88,role='subtitle')
        for name,x,color in [('function-mobile',4200,'#22AEEF'),('function-dashboard',4480,'#F28C28'),('function-sync',4760,'#26B85A')]:
            icon(root,name,x,590,70,color,True)
        assert title_end < 4100, title_end
        assert 1650+subtitle_width < 5000,subtitle_width
    else:
        image(root,'issu-social-wordmark.png',x=38,y=28,width=205,height=105)
        mark=element(root,'svg',x=60,y=175,width=235,height=235,viewBox='0 0 320 320')
        element(mark,'path',id='nodus-project-mark',d=N_PATH,fill=MAGENTA)
        title_end=project_title(root,320,340,100,20)
        subtitle_width=outlined(root,'MINIMAL ANDROID LAUNCHER WITH A NOTIFICATION HUB',320,425,24,role='subtitle')
        for name,x,color in [('function-mobile',1042,'#22AEEF'),('function-dashboard',1124,'#F28C28'),('function-sync',1206,'#26B85A')]:
            icon(root,name,x,80,27.52,color)
        assert title_end < 1240,title_end
        assert 320+subtitle_width < 1240,subtitle_width
    arcs(root,banner)
    stem='nodus-banner' if banner else 'nodus-social-preview'
    ET.ElementTree(root).write(HERE/f'{stem}.svg',encoding='unicode',xml_declaration=True)
    metrics[stem]={'width':w,'height':h,'title_end':round(title_end,2),'subtitle_end':round((1650 if banner else 320)+subtitle_width,2)}
print(json.dumps(metrics,indent=2))
