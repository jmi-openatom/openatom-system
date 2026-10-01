# 江苏海事校园地图数据

首页使用本地 GeoJSON 补充校园建筑和景观，在现有 Mapbox 地图上显示。
`/campus-map` 是全屏展示页，按住空格后鼠标左键拖拽平移，支持右键旋转、滚轮缩放、
触屏双指操作，以及建筑名称搜索、定位、楼名开关和日夜切换。
校园外保持原有灰色建筑，校园内使用浅色楼体、蓝色屋顶、绿色地面、
青色湖水和红色运动场。日间、夜间分别设置配色，缩回地球时校园图层隐藏。

## 数据来源

- `campusMap.ts`：WGS84 校园边界，来源为
  [OpenStreetMap way 92626951](https://www.openstreetmap.org/way/92626951)。
  东侧空地按用户确认扩入校园边界，补充为草地，边界为参考图估计。
- `campusLandscape.json`：2026-10-01 OpenStreetMap 校园周边快照中，完全位于
  校园边界内的湖泊、泳池、绿地、运动场和道路。保留 OSM way ID。
- `campusBuildings.json`：边界内已有的 OSM 建筑轮廓，以及依据用户提供的
  校园手绘图和鸟瞰图补充的北区宿舍、教学楼等示意建筑。
  “念稼食堂”和“沁园5号公寓”由用户确认名称；沁园5号参考用户补充图
  放在沁园4号西侧，四号补充为带庭院的 U 形轮廓。并依据补充截图标注船舶与海洋工程学院、焊接实训室，建筑共 39 栋，山顶灯塔统一归入建筑目录（共 40 项）。
  这两处新增轮廓和位置仍是参考图估计。

`campusLandmark.ts` 补充用户指出的校园小山、盘山步道和灯塔。
山体为平滑曲面，树木高度随山体抬升，并结合 Mapbox 区域 DEM 与周边
地面衔接。小山的 15 米相对高度及灯塔尺寸均为示意值，待实测替换。
`campusLabels.ts` 为每栋建筑生成名称标签和搜索位置；部分公寓名称按
参考图推定，`name_source` 保留名称来源，后续可独立校正。

`data_source: "openstreetmap"` 表示已有地图轮廓，
`data_source: "reference-sketch"` 表示根据参考图补充的示意轮廓。
后者的位置、尺寸和高度均为估计，不能作为测绘或导航依据。
`height_estimated: true` 标识估计高度：有楼层数据时按每层 3.5 米折算，
无高度数据时按建筑用途设置示意高度。

OSM 数据遵循 [ODbL](https://www.openstreetmap.org/copyright)，
地图右下角保留 Mapbox / OpenStreetMap 署名。运行时只读取本地校园数据，
不依赖 OSM / Overpass 在线请求。

`campusDetails.ts` 根据本地建筑轮廓生成窗格、屋顶女儿墙，并在绿地和道路
旁生成树列及运动场标线。树木和建筑细节为艺术化示意，不代表实测数量、
位置或建筑立面。避免在已知建筑、湖面和球场内部布置树木。

## 更新方式

在 GeoJSON 中编辑建筑 `geometry.coordinates`（经度、纬度）和 `height`（米），
可以替换为后续获得的真实建筑轮廓和实测高度；有庭院的建筑使用带内环的多边形。
新增建筑应位于 `CAMPUS_BOUNDARY` 内，保留数据来源和估计高度标记。
景观 `kind` 可为 `park`、`water`、`track`、`court`、`pitch` 或 `road`。
