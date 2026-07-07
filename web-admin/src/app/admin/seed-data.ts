import { Firestore, collection, doc, setDoc, getDocs, deleteDoc } from '@angular/fire/firestore';

export async function seedProducts(firestore: Firestore) {
  const products = [
  {
    "id": "AD01",
    "name": "Khói Trắng Kết Duyên",
    "category": "ao-dai",
    "price": 2890000,
    "images": [
      "https://i.ibb.co/NdrqZhXF/download-id-1-J5-Bk-Vyb0cb1q-BJzou-Ztxg-Okg5-Ea9gh4-S-export-view.jpg",
      "https://i.ibb.co/jv3NCGFB/download-id-1-Xxl-If-Ne-WBFUc-Vlcxl7-a9tvd233-F-Om-export-view.jpg",
      "https://i.ibb.co/ycyLC7rH/download-id-11578pok-Apt88dy-Gee-Ll7-Us-D9h-Sm-NTj-G-export-view.jpg",
      "https://i.ibb.co/1CBnCyD/download-id-1-H1gze-Nlz-TSGJe-B7-RB3-PVmnnp-t-Y4-Jn-JD-export-view.jpg",
      "https://i.ibb.co/N6L4LgBf/download-id-1-M2-UU-o8pv-9y-Vl8-UIos0-Ydz-IZrb3-V6dg-export-view.jpg",
      "https://i.ibb.co/W43bPWhS/Save-Tik-Tok-to-7536953985236880658-4.jpg",
      "https://i.ibb.co/zW7Pj6j3/Save-Tik-Tok-to-7536953985236880658-3.jpg"
    ]
  },
  {
    "id": "AD02",
    "name": "Lam Lụa Cố Trạch",
    "category": "ao-dai",
    "price": 1590000,
    "images": [
      "https://i.ibb.co/jvvBZHMt/Save-Tik-Tok-to-7657078793697299719-18.jpg",
      "https://i.ibb.co/XGWcSX4/Save-Tik-Tok-to-7657078793697299719-10.jpg",
      "https://i.ibb.co/8Dm869Y1/Save-Tik-Tok-to-7657078793697299719-12.jpg",
      "https://i.ibb.co/6cSsHgZv/Save-Tik-Tok-to-7657078793697299719-11.jpg",
      "https://i.ibb.co/JRpVfNrm/Save-Tik-Tok-to-7657078793697299719-8.jpg",
      "https://i.ibb.co/HTM0rPVM/Save-Tik-Tok-to-7657078793697299719-5.jpg",
      "https://i.ibb.co/gbWycMCV/Save-Tik-Tok-to-7657078793697299719-6.jpg",
      "https://i.ibb.co/C56z4HjS/Save-Tik-Tok-to-7657078793697299719-4.jpg",
      "https://i.ibb.co/MxVMpkmG/Save-Tik-Tok-to-7657078793697299719-3.jpg",
      "https://i.ibb.co/n8kHsjPC/Save-Tik-Tok-to-7657078793697299719-2.jpg",
      "https://i.ibb.co/ZpxTXJpk/Save-Tik-Tok-to-7657078793697299719-1.jpg"
    ]
  },
  {
    "id": "AD03",
    "name": "Kim Vũ Phong Hoa",
    "category": "ao-dai",
    "price": 1750000,
    "images": [
      "https://i.ibb.co/Ld74wtdh/Save-Tik-Tok-to-7595437759066819848-18.jpg",
      "https://i.ibb.co/tT08WHxR/Save-Tik-Tok-to-7595437759066819848-6.jpg",
      "https://i.ibb.co/BKY06mtn/Save-Tik-Tok-to-7595437759066819848-3.jpg",
      "https://i.ibb.co/kVGKVxw9/Save-Tik-Tok-to-7595437759066819848-1.jpg",
      "https://i.ibb.co/3gcMkfS/Save-Tik-Tok-to-7595437759066819848-2.jpg"
    ]
  },
  {
    "id": "AD04",
    "name": "Hồng Trần Mộc Dược",
    "category": "ao-dai",
    "price": 1290000,
    "images": [
      "https://i.ibb.co/1JzY22H8/untitled-catalog8585-3df024107b884e94815745a3bc80b696-master.jpg",
      "https://i.ibb.co/ZpRcpfmd/untitled-catalog8427-5c18732a03624b0588b3237d25a1daf8-master.webp",
      "https://i.ibb.co/39br9gq7/untitled-catalog8291-c4ea38f6a7a34042a9305f94e37fe3eb-master.jpg",
      "https://i.ibb.co/cSpGWWym/1-c7215a80b8f340e7a160897d843c0925-master.webp"
    ]
  },
  {
    "id": "AD05",
    "name": "Lục Thuỷ Hoàng Lan",
    "category": "ao-dai",
    "price": 1450000,
    "images": [
      "https://i.ibb.co/fGXSF0YF/Save-Tik-Tok-to-7581119991194340629-8.jpg",
      "https://i.ibb.co/5WccMfYk/Save-Tik-Tok-to-7581119991194340629-7.jpg",
      "https://i.ibb.co/QjC8rSv4/Save-Tik-Tok-to-7581119991194340629-5.jpg",
      "https://i.ibb.co/JR415Rqj/Save-Tik-Tok-to-7581119991194340629-6.jpg",
      "https://i.ibb.co/TMt8LRqk/Save-Tik-Tok-to-7581119991194340629-4.jpg",
      "https://i.ibb.co/wHW4WXW/Save-Tik-Tok-to-7581119991194340629-2.jpg",
      "https://i.ibb.co/RG2ZBRmC/Save-Tik-Tok-to-7581119991194340629-3.jpg",
      "https://i.ibb.co/9m8qmX5F/Save-Tik-Tok-to-7581119991194340629-1.jpg"
    ]
  },
  {
    "id": "AD06",
    "name": "Nguyệt Cầm Phấn Hồng",
    "category": "ao-dai",
    "price": 1350000,
    "images": [
      "https://i.ibb.co/fzHnbM12/Save-Tik-Tok-to-7575396156541357320-18.jpg",
      "https://i.ibb.co/1GZ61RXr/Save-Tik-Tok-to-7575396156541357320-21.jpg",
      "https://i.ibb.co/KcZYNHCJ/Save-Tik-Tok-to-7575396156541357320-16.jpg",
      "https://i.ibb.co/LDFWPQ1b/Save-Tik-Tok-to-7575396156541357320-14.jpg",
      "https://i.ibb.co/chPYSCgR/Save-Tik-Tok-to-7575396156541357320-12.jpg",
      "https://i.ibb.co/xSB6Qzgf/Save-Tik-Tok-to-7575396156541357320-9.jpg",
      "https://i.ibb.co/0Vt5w9MG/Save-Tik-Tok-to-7575396156541357320-10.jpg",
      "https://i.ibb.co/20GmFDGC/Save-Tik-Tok-to-7575396156541357320-8.jpg",
      "https://i.ibb.co/mgVnf70/Save-Tik-Tok-to-7575396156541357320-2.jpg"
    ]
  },
  {
    "id": "AD07",
    "name": "Phấn Hoa Cổ Điển",
    "category": "ao-dai",
    "price": 1490000,
    "images": [
      "https://i.ibb.co/tM3tJrym/lea3735-e91f2728b6db4622837649e7fd28fb3d-master.webp",
      "https://i.ibb.co/yFFfRVmS/lea3614-ea896e94d7a34239a840a94b107322cb-master.webp",
      "https://i.ibb.co/1Y3jpHM7/lea3854-b4ffce46cf1a42bbbb012114920b1738-master.webp",
      "https://i.ibb.co/93cTcG2d/7-a3eddb26dac94603a27321c200a7f5ee-master.webp"
    ]
  },
  {
    "id": "NB01",
    "name": "Xích Bào Đối Ấn",
    "category": "nhat-binh",
    "price": 3490000,
    "images": [
      "https://i.ibb.co/DPDYjXZ6/Save-Tik-Tok-to-7524936880689876242-9.jpg",
      "https://i.ibb.co/wZdMGYxp/Save-Tik-Tok-to-7524936880689876242-8.jpg",
      "https://i.ibb.co/xSnF7nMM/Save-Tik-Tok-to-7524936880689876242-5.jpg",
      "https://i.ibb.co/1YVrvcL6/Save-Tik-Tok-to-7524936880689876242-6.jpg",
      "https://i.ibb.co/9CJ7g6X/Save-Tik-Tok-to-7524936880689876242-2.jpg",
      "https://i.ibb.co/HLQy04hh/Save-Tik-Tok-to-7524936880689876242-3.jpg",
      "https://i.ibb.co/wZVd9fsL/Save-Tik-Tok-to-7524936880689876242-1.jpg"
    ]
  },
  {
    "id": "NB02",
    "name": "Thạch Lam Hoàng Cung",
    "category": "nhat-binh",
    "price": 3290000,
    "images": [
      "https://i.ibb.co/svxNh6b5/Save-Tik-Tok-to-7523499022561201426-10.jpg",
      "https://i.ibb.co/nMwJHTyd/Save-Tik-Tok-to-7523499022561201426-7.jpg",
      "https://i.ibb.co/6RD7bXP9/Save-Tik-Tok-to-7523499022561201426-8.jpg",
      "https://i.ibb.co/rKn4NDgb/Save-Tik-Tok-to-7523499022561201426-11.jpg",
      "https://i.ibb.co/sBJNGvQ/Save-Tik-Tok-to-7523499022561201426-4.jpg",
      "https://i.ibb.co/d0k3Jvv5/Save-Tik-Tok-to-7523499022561201426-6.jpg",
      "https://i.ibb.co/PvH8Z0rd/Save-Tik-Tok-to-7523499022561201426-3.jpg",
      "https://i.ibb.co/G3cx5t96/Save-Tik-Tok-to-7523499022561201426-1.jpg",
      "https://i.ibb.co/VY8jSgmJ/Save-Tik-Tok-to-7523499022561201426-2.jpg"
    ]
  },
  {
    "id": "NB03",
    "name": "Lục Triều Tiểu Yến",
    "category": "nhat-binh",
    "price": 2890000,
    "images": [
      "https://i.ibb.co/VpmSQzrS/Save-Tik-Tok-to-7445598293263928583-10.jpg",
      "https://i.ibb.co/mCrdSkJP/Save-Tik-Tok-to-7445598293263928583-12.jpg",
      "https://i.ibb.co/FkRpSYyB/Save-Tik-Tok-to-7445598293263928583-8.jpg",
      "https://i.ibb.co/67LCsZy9/Save-Tik-Tok-to-7445598293263928583-11.jpg",
      "https://i.ibb.co/VpVMGgz5/Save-Tik-Tok-to-7445598293263928583-5.jpg",
      "https://i.ibb.co/bw2K5MZ/Save-Tik-Tok-to-7445598293263928583-2.jpg",
      "https://i.ibb.co/vxQ03pBb/Save-Tik-Tok-to-7445598293263928583-1.jpg"
    ]
  },
  {
    "id": "NB04",
    "name": "Hoàng Triều Kim Tuyến",
    "category": "nhat-binh",
    "price": 2190000,
    "images": [
      "https://i.ibb.co/TxTdgxyc/Save-Tik-Tok-to-7530128385167740168-10.jpg",
      "https://i.ibb.co/Pvq8qJZc/Save-Tik-Tok-to-7530128385167740168-6.jpg",
      "https://i.ibb.co/TMqX4Jgx/Save-Tik-Tok-to-7530128385167740168-7.jpg",
      "https://i.ibb.co/spXXdDSF/Save-Tik-Tok-to-7530128385167740168-8.jpg",
      "https://i.ibb.co/DH7PcRd2/Save-Tik-Tok-to-7530128385167740168-9.jpg",
      "https://i.ibb.co/C3pFdmtc/Save-Tik-Tok-to-7530128385167740168-3.jpg",
      "https://i.ibb.co/mCnw41bD/Save-Tik-Tok-to-7530128385167740168-4.jpg",
      "https://i.ibb.co/Nng9ksX7/Save-Tik-Tok-to-7530128385167740168-5.jpg",
      "https://i.ibb.co/LzcQDpzp/Save-Tik-Tok-to-7530128385167740168-2.jpg",
      "https://i.ibb.co/vxJgdTWN/Save-Tik-Tok-to-7530128385167740168-1.jpg"
    ]
  },
  {
    "id": "NB05",
    "name": "Vọng Nguyệt Lam Cung",
    "category": "nhat-binh",
    "price": 2690000,
    "images": [
      "https://i.ibb.co/MxZqSwhW/Save-Tik-Tok-to-7609942486915534088-7.jpg",
      "https://i.ibb.co/5WFD6Cqy/Save-Tik-Tok-to-7609942486915534088-8.jpg",
      "https://i.ibb.co/S4X0Dswd/Save-Tik-Tok-to-7609942486915534088-9.jpg",
      "https://i.ibb.co/ddgT3yv/Save-Tik-Tok-to-7609942486915534088-6.jpg",
      "https://i.ibb.co/6002yT15/Save-Tik-Tok-to-7609942486915534088-5.jpg",
      "https://i.ibb.co/Wv7q01B6/Save-Tik-Tok-to-7609942486915534088-3.jpg",
      "https://i.ibb.co/yc7Scw2x/Save-Tik-Tok-to-7609942486915534088-1.jpg"
    ]
  },
  {
    "id": "NB06",
    "name": "Nhật Bình Lam Vũ",
    "category": "nhat-binh",
    "price": 2990000,
    "images": [
      "https://i.ibb.co/HfXjWkQq/1.png",
      "https://i.ibb.co/svVXf0sm/2.png",
      "https://i.ibb.co/60xDwRdL/3.png",
      "https://i.ibb.co/k2fp570C/4.png"
    ]
  },
  {
    "id": "NB07",
    "name": "Lục Yên Lam Bửu",
    "category": "nhat-binh",
    "price": 2750000,
    "images": [
      "https://i.ibb.co/spgGTnxB/Save-Tik-Tok-to-7641230704784837895-5.jpg",
      "https://i.ibb.co/7JxVVYp6/Save-Tik-Tok-to-7641230704784837895-7.jpg",
      "https://i.ibb.co/84BGV1M8/Save-Tik-Tok-to-7641230704784837895-9.jpg",
      "https://i.ibb.co/20mPp0hz/Save-Tik-Tok-to-7641230704784837895-3.jpg",
      "https://i.ibb.co/q3wmgXYM/Save-Tik-Tok-to-7641230704784837895-4.jpg",
      "https://i.ibb.co/8ny9PH1y/Save-Tik-Tok-to-7641230704784837895-8.jpg",
      "https://i.ibb.co/fdjqQ72s/Save-Tik-Tok-to-7641230704784837895-2.jpg"
    ]
  },
  {
    "id": "NB08",
    "name": "Tử Vân Yên Thảo",
    "category": "nhat-binh",
    "price": 2450000,
    "images": [
      "https://i.ibb.co/r2xqthKf/615886106-1338778654932127-6981343839607239234-n-1.jpg",
      "https://i.ibb.co/DfLNTRWd/615694030-1338778418265484-8337428161423113752-n-1.jpg",
      "https://i.ibb.co/GvzJGgVc/615828918-1338778431598816-5152446385326370960-n.jpg",
      "https://i.ibb.co/Lh2xT31t/617732872-1338778438265482-5894765264226452245-n.jpg",
      "https://i.ibb.co/6Jgphx3M/617156194-1338778524932140-345819971237651444-n.jpg",
      "https://i.ibb.co/fY3rd5kh/616739915-1338778561598803-8935526165360481638-n.jpg",
      "https://i.ibb.co/twCH4z3N/616197856-1338778541598805-2533228097339180681-n.jpg"
    ]
  },
  {
    "id": "NB09",
    "name": "Trầm Hồng Cổ Các",
    "category": "nhat-binh",
    "price": 2150000,
    "images": [
      "https://i.ibb.co/6cvjNqRm/reelsvideo-io-1782906346789.jpg",
      "https://i.ibb.co/673M16zz/reelsvideo-io-1782906327306.jpg",
      "https://i.ibb.co/k2kst4rs/reelsvideo-io-1782906324783.jpg",
      "https://i.ibb.co/8nPZhKMh/reelsvideo-io-1782906322411.jpg"
    ]
  },
  {
    "id": "NB10",
    "name": "Kim Trần Mộc Dược",
    "category": "nhat-binh",
    "price": 1890000,
    "images": [
      "https://i.ibb.co/C5gccxdG/Save-Tik-Tok-to-7529138817652051218-10.jpg",
      "https://i.ibb.co/yBgr4rjW/Save-Tik-Tok-to-7529138817652051218-9.jpg",
      "https://i.ibb.co/RTCKDv0n/Save-Tik-Tok-to-7529138817652051218-7.jpg",
      "https://i.ibb.co/V0qWVpnY/Save-Tik-Tok-to-7529138817652051218-8.jpg",
      "https://i.ibb.co/LXSQwTFN/Save-Tik-Tok-to-7529138817652051218-5.jpg",
      "https://i.ibb.co/V0m2BVvb/Save-Tik-Tok-to-7529138817652051218-6.jpg",
      "https://i.ibb.co/V0dBQPBt/Save-Tik-Tok-to-7529138817652051218-2.jpg",
      "https://i.ibb.co/QFSDbLfT/Save-Tik-Tok-to-7529138817652051218-3.jpg"
    ]
  },
  {
    "id": "AT01",
    "name": "Lục Ngọc Vấn Khăn",
    "category": "ao-tac",
    "price": 1590000,
    "images": [
      "https://i.ibb.co/8Ct5dK1/Save-Tik-Tok-to-7657367171604958485-10.jpg",
      "https://i.ibb.co/7JQWs2QG/Save-Tik-Tok-to-7657367171604958485-8.jpg",
      "https://i.ibb.co/yFd1dYff/Save-Tik-Tok-to-7657367171604958485-9.jpg",
      "https://i.ibb.co/7xYCfLqS/Save-Tik-Tok-to-7657367171604958485-7.jpg",
      "https://i.ibb.co/KYH50ns/Save-Tik-Tok-to-7657367171604958485-4.jpg",
      "https://i.ibb.co/bM1zHSjH/Save-Tik-Tok-to-7657367171604958485-2.jpg",
      "https://i.ibb.co/xKPwvFrF/Save-Tik-Tok-to-7657367171604958485-1.jpg"
    ]
  },
  {
    "id": "AT02",
    "name": "Ngọc Vũ Yên Sa",
    "category": "ao-tac",
    "price": 1890000,
    "images": [
      "https://i.ibb.co/fY3NrpC6/Save-Tik-Tok-to-7589892978848763156-12.jpg",
      "https://i.ibb.co/xqd51cVL/Save-Tik-Tok-to-7589892978848763156-10.jpg",
      "https://i.ibb.co/JRNYht8M/Save-Tik-Tok-to-7589892978848763156-11.jpg",
      "https://i.ibb.co/QFWzkkPT/Save-Tik-Tok-to-7589892978848763156-7.jpg",
      "https://i.ibb.co/jZrTxQzx/Save-Tik-Tok-to-7589892978848763156-4.jpg",
      "https://i.ibb.co/RpCzQ140/Save-Tik-Tok-to-7589892978848763156-5.jpg",
      "https://i.ibb.co/qYGsdnHF/Save-Tik-Tok-to-7589892978848763156-6.jpg",
      "https://i.ibb.co/cXQhx0Pg/Save-Tik-Tok-to-7589892978848763156-3.jpg",
      "https://i.ibb.co/tTHWM4Bd/Save-Tik-Tok-to-7589892978848763156-1.jpg",
      "https://i.ibb.co/VWZz8BHy/Save-Tik-Tok-to-7589892978848763156-2.jpg"
    ]
  },
  {
    "id": "AT03",
    "name": "Mộc Vân Thổ Xà",
    "category": "ao-tac",
    "price": 1290000,
    "images": [
      "https://i.ibb.co/ZR5MbyTf/Save-Tik-Tok-to-7527510246911233288-8.jpg",
      "https://i.ibb.co/w5891bT/Save-Tik-Tok-to-7527510246911233288-9.jpg",
      "https://i.ibb.co/w5tgbLh/Save-Tik-Tok-to-7527510246911233288-5.jpg",
      "https://i.ibb.co/HTTr0rqt/Save-Tik-Tok-to-7527510246911233288-4.jpg",
      "https://i.ibb.co/Wbc72pn/Save-Tik-Tok-to-7527510246911233288-2.jpg",
      "https://i.ibb.co/tTxcg4nK/Save-Tik-Tok-to-7527510246911233288-3.jpg"
    ]
  },
  {
    "id": "AT04",
    "name": "Thanh Long Cổ Trấn",
    "category": "ao-tac",
    "price": 1450000,
    "images": [
      "https://i.ibb.co/XZ8Q1Y3G/Save-Tik-Tok-to-7597785204538821895-14.jpg",
      "https://i.ibb.co/gbNgKg96/Save-Tik-Tok-to-7597785204538821895-9.jpg",
      "https://i.ibb.co/S4m57PSQ/Save-Tik-Tok-to-7597785204538821895-7.jpg",
      "https://i.ibb.co/d4sJQhPC/Save-Tik-Tok-to-7597785204538821895-4.jpg",
      "https://i.ibb.co/hxWVtzsJ/Save-Tik-Tok-to-7597785204538821895-5.jpg",
      "https://i.ibb.co/bRvXxcmN/Save-Tik-Tok-to-7597785204538821895-6.jpg",
      "https://i.ibb.co/60R8fnHK/Save-Tik-Tok-to-7597785204538821895-3.jpg",
      "https://i.ibb.co/ch6qSqP8/Save-Tik-Tok-to-7597785204538821895-1.jpg",
      "https://i.ibb.co/RTm2qj7h/Save-Tik-Tok-to-7597785204538821895-2.jpg"
    ]
  },
  {
    "id": "AT05",
    "name": "Lục Y Phù Quạt",
    "category": "ao-tac",
    "price": 1690000,
    "images": [
      "https://i.ibb.co/FbR1S0yV/Save-Tik-Tok-to-7433004520994540807-14.jpg",
      "https://i.ibb.co/Sk7RC8r/Save-Tik-Tok-to-7433004520994540807-12.jpg",
      "https://i.ibb.co/PvB8pPS2/Save-Tik-Tok-to-7433004520994540807-9.jpg",
      "https://i.ibb.co/bjvpKsJH/Save-Tik-Tok-to-7433004520994540807-10.jpg",
      "https://i.ibb.co/fGQQVvXk/Save-Tik-Tok-to-7433004520994540807-11.jpg",
      "https://i.ibb.co/7dMJyjjy/Save-Tik-Tok-to-7433004520994540807-6.jpg",
      "https://i.ibb.co/qFFCNbvY/Save-Tik-Tok-to-7433004520994540807-5.jpg",
      "https://i.ibb.co/qMtG5TyF/Save-Tik-Tok-to-7433004520994540807-3.jpg",
      "https://i.ibb.co/67tshZ4V/Save-Tik-Tok-to-7433004520994540807-2.jpg",
      "https://i.ibb.co/Ldrg672b/Save-Tik-Tok-to-7433004520994540807-1.jpg"
    ]
  },
  {
    "id": "AT06",
    "name": "Tơ Ngà Vấn Nguyệt",
    "category": "ao-tac",
    "price": 1750000,
    "images": [
      "https://i.ibb.co/MDWLmynd/Save-Tik-Tok-to-7657033148290600212-10.jpg",
      "https://i.ibb.co/rRnz80CN/Save-Tik-Tok-to-7657033148290600212-12.jpg",
      "https://i.ibb.co/G4DHnm84/Save-Tik-Tok-to-7657033148290600212-7.jpg",
      "https://i.ibb.co/QFnbg2RT/Save-Tik-Tok-to-7657033148290600212-8.jpg",
      "https://i.ibb.co/v6fr6MNL/Save-Tik-Tok-to-7657033148290600212-4.jpg",
      "https://i.ibb.co/6RGFF5qf/Save-Tik-Tok-to-7657033148290600212-6.jpg",
      "https://i.ibb.co/CKc75QNc/Save-Tik-Tok-to-7657033148290600212-1.jpg",
      "https://i.ibb.co/Ld9Npm6V/Save-Tik-Tok-to-7657033148290600212-2.jpg",
      "https://i.ibb.co/FqqWmrQN/Save-Tik-Tok-to-7657033148290600212-3.jpg"
    ]
  },
  {
    "id": "GL01",
    "name": "Bạch Sa Liên Vũ",
    "category": "giao-linh",
    "price": 2290000,
    "images": [
      "https://i.ibb.co/TxpypjR9/Save-Tik-Tok-to-7555360886152547592-1.jpg",
      "https://i.ibb.co/fGqXfXNN/Save-Tik-Tok-to-7555360886152547592-3.jpg",
      "https://i.ibb.co/jxwvKsS/Save-Tik-Tok-to-7555360886152547592-4.jpg",
      "https://i.ibb.co/JFH3t44K/Save-Tik-Tok-to-7555360886152547592-5.jpg",
      "https://i.ibb.co/8L59CBbg/Save-Tik-Tok-to-7555360886152547592-6.jpg",
      "https://i.ibb.co/mC4vXK8C/Save-Tik-Tok-to-7555360886152547592-7.jpg",
      "https://i.ibb.co/XrP8DMgC/Save-Tik-Tok-to-7555360886152547592-8.jpg",
      "https://i.ibb.co/VWhYFYtV/Save-Tik-Tok-to-7555360886152547592-9.jpg",
      "https://i.ibb.co/35yg30GM/Save-Tik-Tok-to-7555360886152547592-10.jpg",
      "https://i.ibb.co/SDnkDXSj/Save-Tik-Tok-to-7555360886152547592-12.jpg"
    ]
  },
  {
    "id": "GL02",
    "name": "Lam Ngọc Cổ Trấn",
    "category": "giao-linh",
    "price": 2490000,
    "images": [
      "https://i.ibb.co/j92jcSb9/Save-Tik-Tok-to-7496693802669837575-10.jpg",
      "https://i.ibb.co/bg42J0FX/Save-Tik-Tok-to-7496693802669837575-7.jpg",
      "https://i.ibb.co/7dD34BJK/Save-Tik-Tok-to-7496693802669837575-9.jpg",
      "https://i.ibb.co/8Ds5nWX0/Save-Tik-Tok-to-7496693802669837575-4.jpg",
      "https://i.ibb.co/21bD6vKV/Save-Tik-Tok-to-7496693802669837575-5.jpg",
      "https://i.ibb.co/sJb3JRBv/Save-Tik-Tok-to-7496693802669837575-6.jpg",
      "https://i.ibb.co/Ngr0CXM6/Save-Tik-Tok-to-7496693802669837575-1.jpg",
      "https://i.ibb.co/RGyJ8bDQ/Save-Tik-Tok-to-7496693802669837575-3.jpg"
    ]
  },
  {
    "id": "GL03",
    "name": "Lục Trúc Vân Khúc",
    "category": "giao-linh",
    "price": 2450000,
    "images": [
      "https://i.ibb.co/RT7ScYFv/1.png",
      "https://i.ibb.co/4wMq68hR/2.png",
      "https://i.ibb.co/gMNG77RG/3.png",
      "https://i.ibb.co/pBsmDY2M/4.png",
      "https://i.ibb.co/YBrm05LL/5.png",
      "https://i.ibb.co/ksdz0nkz/6.png"
    ]
  },
  {
    "id": "GL04",
    "name": "Kim Sắc Hoàng Triều",
    "category": "giao-linh",
    "price": 2290000,
    "images": [
      "https://i.ibb.co/ZR5MbyTf/Save-Tik-Tok-to-7527510246911233288-8.jpg",
      "https://i.ibb.co/w5891bT/Save-Tik-Tok-to-7527510246911233288-9.jpg",
      "https://i.ibb.co/w5tgbLh/Save-Tik-Tok-to-7527510246911233288-5.jpg",
      "https://i.ibb.co/HTTr0rqt/Save-Tik-Tok-to-7527510246911233288-4.jpg",
      "https://i.ibb.co/Wbc72pn/Save-Tik-Tok-to-7527510246911233288-2.jpg",
      "https://i.ibb.co/tTxcg4nK/Save-Tik-Tok-to-7527510246911233288-3.jpg"
    ]
  },
  {
    "id": "GL05",
    "name": "Cam Giao Lĩnh Bào",
    "category": "giao-linh",
    "price": 1890000,
    "images": [
      "https://i.ibb.co/wq5BShh/ec510c73efe92957fa40d304a56158b6.jpg",
      "https://i.ibb.co/FLvT9HPZ/9daaecc8f540bd017714b80fa9027aee.jpg",
      "https://i.ibb.co/xtHynvnY/c3d108731198d8432793f90b4ccbf521.jpg",
      "https://i.ibb.co/gZCvLPPg/c4fb42089efa7890047f0a13d8d70e65.jpg"
    ]
  },
  {
    "id": "GL06",
    "name": "Hắc Kim Mẫu Đơn",
    "category": "giao-linh",
    "price": 2690000,
    "images": [
      "https://i.ibb.co/whPGkynb/Save-Tik-Tok-to-7563166524735786248-7.jpg",
      "https://i.ibb.co/8LwjVq38/Save-Tik-Tok-to-7563166524735786248-8.jpg",
      "https://i.ibb.co/zCQ5pXG/Save-Tik-Tok-to-7563166524735786248-4.jpg",
      "https://i.ibb.co/8n07bgxF/Save-Tik-Tok-to-7563166524735786248-5.jpg",
      "https://i.ibb.co/gZZxJgzV/Save-Tik-Tok-to-7563166524735786248-2.jpg",
      "https://i.ibb.co/jZ69WtgT/Save-Tik-Tok-to-7563166524735786248-3.jpg",
      "https://i.ibb.co/sfvm8TP/Save-Tik-Tok-to-7563166524735786248-1.jpg"
    ]
  },
  {
    "id": "YD01",
    "name": "Sương Mai Bạch Vũ",
    "category": "yem-dao",
    "price": 2290000,
    "images": [
      "https://i.ibb.co/prWJp7rh/Save-Tik-Tok-to-7556868754689232135-11.jpg",
      "https://i.ibb.co/8L16TJp4/Save-Tik-Tok-to-7556868754689232135-9-1.jpg",
      "https://i.ibb.co/bM4WgkGw/Save-Tik-Tok-to-75556868754689232135-5.jpg",
      "https://i.ibb.co/jZVcrj1n/Save-Tik-Tok-to-7556868754689232135-6.jpg",
      "https://i.ibb.co/zVrjwq9Y/Save-Tik-Tok-to-75556868754689232135-8.jpg",
      "https://i.ibb.co/HpGtzgX4/Save-Tik-Tok-to-7556868754689232135-3.jpg",
      "https://i.ibb.co/0jTXqdmK/Save-Tik-Tok-to-7556868754689232135-4.jpg",
      "https://i.ibb.co/qLjvvDCq/Save-Tik-Tok-to-7556868754689232135-2.jpg"
    ]
  },
  {
    "id": "YD02",
    "name": "Trúc Lục Khuê Phòng",
    "category": "yem-dao",
    "price": 2690000,
    "images": [
      "https://i.ibb.co/PzJKNT4J/Save-Tik-Tok-to-7490487015885049093-10.jpg",
      "https://i.ibb.co/GvjFXpc8/Save-Tik-Tok-to-7490487015885049093-6.jpg",
      "https://i.ibb.co/bjLVwrLH/Save-Tik-Tok-to-7490487015885049093-7.jpg",
      "https://i.ibb.co/zV13rTff/Save-Tik-Tok-to-7490487015885049093-8.jpg",
      "https://i.ibb.co/7tw0Ybyd/Save-Tik-Tok-to-7490487015885049093-9.jpg",
      "https://i.ibb.co/BKTf0W41/Save-Tik-Tok-to-7490487015885049093-5.jpg",
      "https://i.ibb.co/HDhRbzd9/Save-Tik-Tok-to-7490487015885049093-3.jpg",
      "https://i.ibb.co/FkxwgJSh/Save-Tik-Tok-to-7490487015885049093-4.jpg",
      "https://i.ibb.co/mVtK6hcp/Save-Tik-Tok-to-7490487015885049093-1.jpg",
      "https://i.ibb.co/4wGBC0Gd/Save-Tik-Tok-to-7490487015885049093-2.jpg"
    ]
  },
  {
    "id": "YD03",
    "name": "Yên Hoa Bạch Liên",
    "category": "yem-dao",
    "price": 1990000,
    "images": [
      "https://i.ibb.co/N6SGW8rM/Save-Tik-Tok-to-7657422293223705864-6.jpg",
      "https://i.ibb.co/dwJMjGrn/Save-Tik-Tok-to-7657422293223705864-7.jpg",
      "https://i.ibb.co/Q3QvZ7s9/Save-Tik-Tok-to-7657422293223705864-8.jpg",
      "https://i.ibb.co/cSZZCz6n/Save-Tik-Tok-to-7657422293223705864-9.jpg",
      "https://i.ibb.co/4Rvv1VFW/Save-Tik-Tok-to-7657422293223705864-5.jpg",
      "https://i.ibb.co/5Xst8V1d/Save-Tik-Tok-to-7657422293223705864-3.jpg"
    ]
  },
  {
    "id": "YD04",
    "name": "Thanh Lam Trì Liên",
    "category": "yem-dao",
    "price": 2350000,
    "images": [
      "https://i.ibb.co/xSHhKqtP/Save-Tik-Tok-to-7655623573230537992-15.jpg",
      "https://i.ibb.co/N6vqHmBb/Save-Tik-Tok-to-7655623573230537992-10.jpg",
      "https://i.ibb.co/wF9KQjWD/Save-Tik-Tok-to-7655623573230537992-7.jpg",
      "https://i.ibb.co/svTWPbXY/Save-Tik-Tok-to-7655623573230537992-9.jpg",
      "https://i.ibb.co/SDGj9cwg/Save-Tik-Tok-to-7655623573230537992-4.jpg",
      "https://i.ibb.co/GQNBDMq4/Save-Tik-Tok-to-7655623573230537992-6.jpg",
      "https://i.ibb.co/jZBcMwfj/Save-Tik-Tok-to-7655623573230537992-3.jpg",
      "https://i.ibb.co/BVj9Q1fx/Save-Tik-Tok-to-7655623573230537992-1.jpg"
    ]
  },
  {
    "id": "YD05",
    "name": "Bích Lam Cẩm Tú",
    "category": "yem-dao",
    "price": 2450000,
    "images": [
      "https://i.ibb.co/XfGPcBGt/Save-Tik-Tok-to-7555366823559171335-8.jpg",
      "https://i.ibb.co/d4ZytFHZ/Save-Tik-Tok-to-7555366823559171335-9.jpg",
      "https://i.ibb.co/JYqCKGk/Save-Tik-Tok-to-7555366823559171335-6.jpg",
      "https://i.ibb.co/21rbb8c8/Save-Tik-Tok-to-7555366823559171335-7.jpg",
      "https://i.ibb.co/nM9rkcj7/Save-Tik-Tok-to-7555366823559171335-3.jpg",
      "https://i.ibb.co/CK9kSc67/Save-Tik-Tok-to-7555366823559171335-1.jpg"
    ]
  },
  {
    "id": "YD06",
    "name": "Dạ Kim Mẫu Đơn",
    "category": "yem-dao",
    "price": 2890000,
    "images": [
      "https://i.ibb.co/x8FMhQBy/Save-Tik-Tok-to-7639214278481956104-13.jpg",
      "https://i.ibb.co/YFShvDPr/Save-Tik-Tok-to-7639214278481956104-14.jpg",
      "https://i.ibb.co/zVh1LTxZ/Save-Tik-Tok-to-7639214278481956104-12.jpg",
      "https://i.ibb.co/j94f1Mqj/Save-Tik-Tok-to-7639214278481956104-11.jpg"
    ]
  },
  {
    "id": "PK01",
    "name": "Vấn Nguyệt Bạch Vân Cẩm",
    "category": "phu-kien",
    "price": 350000,
    "images": [
      "https://i.ibb.co/vxtCQzds/Save-Tik-Tok-to-7524936880689876242-4.jpg",
      "https://i.ibb.co/xSnF7nMM/Save-Tik-Tok-to-7524936880689876242-5.jpg",
      "https://i.ibb.co/1YVrvcL6/Save-Tik-Tok-to-7524936880689876242-6.jpg",
      "https://i.ibb.co/9CJ7g6X/Save-Tik-Tok-to-7524936880689876242-2.jpg",
      "https://i.ibb.co/wZVd9fsL/Save-Tik-Tok-to-7524936880689876242-1.jpg"
    ]
  },
  {
    "id": "PK02",
    "name": "Nón Dâu Cổ Phong",
    "category": "phu-kien",
    "price": 80000,
    "images": [
      "https://i.ibb.co/dJQ6Frw0/Save-Tik-Tok-to-7642198022130076935-9.jpg",
      "https://i.ibb.co/6RtWb0ch/Save-Tik-Tok-to-7642198022130076935-12.jpg",
      "https://i.ibb.co/gbFsT4gm/Save-Tik-Tok-to-7642198022130076935-8.jpg",
      "https://i.ibb.co/C3G0yF1p/Save-Tik-Tok-to-7642198022130076935-6.jpg",
      "https://i.ibb.co/VYm6PsLg/Save-Tik-Tok-to-7642198022130076935-3.jpg",
      "https://i.ibb.co/q3tkyf5H/Save-Tik-Tok-to-7642198022130076935-4.jpg",
      "https://i.ibb.co/VpBqZks2/Save-Tik-Tok-to-7642198022130076935-5.jpg",
      "https://i.ibb.co/KjQjPTzJ/Save-Tik-Tok-to-7642198022130076935-2.jpg",
      "https://i.ibb.co/Vc0rjs0h/Save-Tik-Tok-to-7642198022130076935-1.jpg"
    ]
  },
  {
    "id": "PK03",
    "name": "Quạt Xếp Khổng Tước Khai Bình",
    "category": "phu-kien",
    "price": 420000,
    "images": [
      "https://i.ibb.co/VcBwjBZy/Save-Tik-Tok-to-7594853782748630279-14.jpg",
      "https://i.ibb.co/ZRNz2rXV/Save-Tik-Tok-to-7594853782748630279-15.jpg",
      "https://i.ibb.co/7xNX8L4N/Save-Tik-Tok-to-7594853782748630279-8.jpg",
      "https://i.ibb.co/fVkXqhCS/Save-Tik-Tok-to-7594853782748630279-5.jpg",
      "https://i.ibb.co/TDqsLLKb/Save-Tik-Tok-to-7594853782748630279-6.jpg",
      "https://i.ibb.co/Y70pmPJh/Save-Tik-Tok-to-7594853782748630279-2.jpg",
      "https://i.ibb.co/Z1L3q9Pk/Save-Tik-Tok-to-7594853782748630279-1.jpg"
    ]
  },
  {
    "id": "PK04",
    "name": "Lọng Tán Trường An",
    "category": "phu-kien",
    "price": 650000,
    "images": [
      "https://i.ibb.co/hJShcFt5/00431b24dc8a032dd7385480dfe111fc.jpg",
      "https://i.ibb.co/rKQC1Qxx/b61b53ff6d6f523d76fe143c244608b4.jpg",
      "https://i.ibb.co/XfSfSp6w/1e1dc2643505d4848dfcfbc3d9903fa2.jpg",
      "https://i.ibb.co/tM9p9DC9/fb0199adab9137f200bc223620ddb3cc.jpg"
    ]
  },
  {
    "id": "PK05",
    "name": "Khăn Vấn Thổ Mộc",
    "category": "phu-kien",
    "price": 890000,
    "images": [
      "https://i.ibb.co/0jxt25z5/4ce34159f36df0ad8a331c0ce11fa01c.jpg",
      "https://i.ibb.co/DDmj2q6c/8255caeeef2968f443e880e8cca756f1.jpg",
      "https://i.ibb.co/23xYM3fc/e2f9388afc9630cf9a58ee62a55d81f8.jpg",
      "https://i.ibb.co/tpLTvtfm/a0d9f2b8ca8f7f1acbbbb7735eb0d9c4.jpg",
      "https://i.ibb.co/NgTbWGgS/46b322fe7a77c1b768bc0cf2b96a4023.jpg",
      "https://i.ibb.co/BKvPnpf1/81599740d2fb2889083e1538a4640ac5.jpg"
    ]
  },
  {
    "id": "PK06",
    "name": "Vân Kiên Đám Mây",
    "category": "phu-kien",
    "price": 120000,
    "images": [
      "https://i.ibb.co/dw7ZHLCr/1.png",
      "https://i.ibb.co/Zzfz48dn/2.png",
      "https://i.ibb.co/kVL6b4Xg/3.png",
      "https://i.ibb.co/TBNPxSXt/4.png"
    ]
  },
  {
    "id": "PK07",
    "name": "Quan Phượng Vũ Triều Thiên",
    "category": "phu-kien",
    "price": 990000,
    "images": [
      "https://i.ibb.co/JZYHrgj/Save-Tik-Tok-to-7640032562852777234-11.jpg",
      "https://i.ibb.co/KcMJQL9W/Save-Tik-Tok-to-7640032562852777234-9.jpg",
      "https://i.ibb.co/0jdZW0bT/Save-Tik-Tok-to-7640032562852777234-6.jpg",
      "https://i.ibb.co/XfLbXHVk/Save-Tik-Tok-to-7640032562852777234-3.jpg",
      "https://i.ibb.co/4RHztkTL/Save-Tik-Tok-to-7640032562852777234-4.jpg",
      "https://i.ibb.co/bj1PbxKr/Save-Tik-Tok-to-7640032562852777234-1.jpg",
      "https://i.ibb.co/B5cfRhPb/Save-Tik-Tok-to-7640032562852777234-2.jpg"
    ]
  },
  {
    "id": "PK08",
    "name": "Ngự Lạp Kim Sa",
    "category": "phu-kien",
    "price": 350000,
    "images": [
      "https://i.ibb.co/fzfhm0gP/Save-Tik-Tok-to-7595437759066819848-15.jpg",
      "https://i.ibb.co/gcQWwv1/Save-Tik-Tok-to-7595437759066819848-16.jpg",
      "https://i.ibb.co/F4mn7Cv9/Save-Tik-Tok-to-7595437759066819848-12.jpg",
      "https://i.ibb.co/bhgFMFf/Save-Tik-Tok-to-7595437759066819848-14.jpg",
      "https://i.ibb.co/7tnVX5n3/Save-Tik-Tok-to-7595437759066819848-9.jpg"
    ]
  },
  {
    "id": "PK09",
    "name": "Lọng Ngự Tán Kim Vân",
    "category": "phu-kien",
    "price": 350000,
    "images": [
      "https://i.ibb.co/sdTns6KM/Save-Tik-Tok-to-7618193923692973332-21.jpg",
      "https://i.ibb.co/d0wSZ2Zm/Save-Tik-Tok-to-7618193923692973332-19.jpg",
      "https://i.ibb.co/LzMf13P0/Save-Tik-Tok-to-7618193923692973332-20.jpg",
      "https://i.ibb.co/JwZGqqZT/Save-Tik-Tok-to-7618193923692973332-9.jpg",
      "https://i.ibb.co/Hp2NcmGr/Save-Tik-Tok-to-7618193923692973332-8.jpg",
      "https://i.ibb.co/svT0zPcd/Save-Tik-Tok-to-7618193923692973332-6.jpg",
      "https://i.ibb.co/Tx4G4HC5/Save-Tik-Tok-to-7618193923692973332-5.jpg"
    ]
  }
];

  // 1. Delete all existing products to ensure clean update
  const productsCol = collection(firestore, 'products');
  const snap = await getDocs(productsCol);
  for (const d of snap.docs) {
    await deleteDoc(d.ref);
  }

  // 2. Seed ALL 45 products with correct categories and collections of images
  for (const p of products) {
    await setDoc(doc(productsCol, p.id), {
      _id: p.id,
      product_name: p.name,
      product_dept: p.category, // Matches your new dropdown categories
      unit_price: p.price,
      stock: 20,
      images: p.images,
      description: 'Sản phẩm Việt Phục TiredCity cao cấp.',
      material: 'Lụa / Gấm truyền thống',
      origin: 'Việt Nam',
      rating: 4.8,
      discount: 0,
      sizes: [
        { size: 'S', quantity: 10 },
        { size: 'M', quantity: 10 },
        { size: 'L', quantity: 10 },
        { size: 'XL', quantity: 10 }
      ]
    });
  }

  // Seed shipping methods
  const shippingCol = collection(firestore, 'shipping_methods');
  const methods = [
    { id: 'ship_01', name: 'Giao hàng Hỏa tốc', fee: 50000, estimatedTime: '2 giờ' },
    { id: 'ship_02', name: 'Giao hàng Nhanh', fee: 30000, estimatedTime: '2-3 ngày' },
    { id: 'ship_03', name: 'Giao hàng Tiết kiệm', fee: 15000, estimatedTime: '4-6 ngày' }
  ];

  for (const s of methods) {
    await setDoc(doc(shippingCol, s.id), s);
  }

  alert('✅ ĐÃ CẬP NHẬT ĐẦY ĐỦ 100% SẢN PHẨM & HÌNH ẢNH THẬT TỪ SHEET!');
}
