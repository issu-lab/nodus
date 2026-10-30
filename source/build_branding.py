"""Apply approved 2026-10-30 lowercase n and theme colors; retain wordmark outlines."""
from pathlib import Path
from copy import deepcopy
import xml.etree.ElementTree as ET
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.svgLib.path import parse_path

ROOT = Path(__file__).resolve().parents[1]
NS = '{http://www.w3.org/2000/svg}'
ET.register_namespace('', NS[1:-1])
N = 'M0 240 V18 Q0 8 10 8 H38 Q48 8 48 18 V22 Q48 28 54 23 C74 8 98 0 124 0 Q224 0 224 106 V224 Q224 240 208 240 H172 V111 Q172 52 121 52 Q83 52 58 73 Q52 78 52 86 V224 Q52 240 36 240 Z'
pen = SVGPathPen(None)
parse_path(N, TransformPen(pen, (1,0,0,1,48,40)))
path = pen.getCommands()
folder = ROOT/'assets/branding/source'
icon = ET.Element(NS+'svg', {'viewBox':'0 0 320 320','width':'1024','height':'1024','role':'img','aria-label':'noduS'})
ET.SubElement(icon,NS+'title').text = 'noduS — n minuscola approvata'
ET.SubElement(icon,NS+'desc').text = 'n morbida con raccordi rifiniti, senza tagli; #F800FF. Approvata il 30 ottobre 2026.'
ET.SubElement(icon,NS+'path',{'d':path,'fill':'#F800FF'})
ET.ElementTree(icon).write(folder/'nodus-icon.svg',encoding='utf-8',xml_declaration=True)
master=ET.parse(folder/'nodus-logo.svg').getroot()
for theme,fg,gray in [('dark','#FFFFFF','#A3A5B1'),('light','#000000','#656976')]:
    logo=deepcopy(master)
    for e in logo:
        if e.tag==NS+'desc':e.text='n minuscola approvata, nodu #F800FF, S storica '+('bianca' if theme=='dark' else 'nera')+' con spaziatura originale; launcher e iSSU Open Homelab.'
    shapes=[e for e in logo if e.tag in (NS+'g',NS+'path')]
    shapes[0].set('d',path)
    shapes[0].set('fill','#F800FF')
    shapes[1].set('fill','#F800FF')
    shapes[2].set('fill',fg)
    for e in shapes[3:]:e.set('fill',gray)
    filename='nodus-logo.svg' if theme=='dark' else 'nodus-logo-light.svg'
    ET.ElementTree(logo).write(folder/filename,encoding='utf-8',xml_declaration=True)
android=ROOT/'app/src/main/res/drawable/nodus_icon.xml'
android.write_text('<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="108dp" android:height="108dp" android:viewportWidth="320" android:viewportHeight="320">\n    <group android:pivotX="160" android:pivotY="160" android:scaleX="0.76" android:scaleY="0.76">\n        <path android:fillColor="#F800FF" android:pathData="'+path+'" />\n    </group>\n</vector>\n')
print('Approved lowercase n, theme wordmarks and Android icon generated.')
