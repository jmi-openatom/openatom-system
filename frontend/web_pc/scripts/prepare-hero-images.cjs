const path = require('path')

const sharp = require('sharp')

const generatedRoot =
  '/Users/ariven/.codex/generated_images/019f9f2a-7627-7240-ac05-dab4284ced31'
const outputRoot = path.resolve(__dirname, '../public')

const images = [
  ['activities-hero-wide-light.png', 'exec-d78fcebd-5b38-4cf6-9ac5-089a30b2a69c.png'],
  ['activities-hero-wide-dark.png', 'exec-418b9ff5-f702-459b-bfd2-2215715acf9c.png'],
  ['blog-hero-wide-light.png', 'exec-f2c2550b-6f1e-45ad-bbc3-13cf042e0c2c.png'],
  ['blog-hero-wide-dark.png', 'exec-71d516d9-2928-4ab4-9e1a-c1451bd40626.png'],
  ['members-hero-wide-light.png', 'exec-5012409c-1f09-40a6-8cc7-923912047e36.png'],
  ['members-hero-wide-dark.png', 'exec-75dc68e8-355e-4b2e-a5e4-56584bc829c2.png'],
  ['partners-hero-wide-light.png', 'exec-ad828c4b-f194-4ca4-b610-240ec212535e.png'],
  ['partners-hero-wide-dark.png', 'exec-13521de6-cc1f-4dd7-b195-0d40dc145e2b.png'],
  ['apps-hero-wide-light.png', 'exec-645efe20-7a6a-454e-8a49-18d0c2661442.png'],
  ['apps-hero-wide-dark.png', 'exec-ef8e9dd7-fc7c-4ec7-900a-15c51a1bf445.png'],
  ['leaves-hero-wide-light.png', 'exec-5cf3ce90-8129-461e-a3ea-448dc4ab11a2.png'],
  ['leaves-hero-wide-dark.png', 'exec-2e6e4582-28a2-416b-9be6-bf9d5998bde1.png'],
  ['recruitment-hero-wide-light.png', 'exec-c5d404a7-da23-4cc3-8ecf-e79a4e7dd587.png'],
  ['recruitment-hero-wide-dark.png', 'exec-a75733de-931d-47c8-97dd-954d12ecd95d.png'],
  ['regulations-hero-wide-light.png', 'exec-4f68e197-a129-45f9-ad46-9d80da95fb69.png'],
  ['regulations-hero-wide-dark.png', 'exec-6f8e5032-1551-4e25-8351-996fb29b501e.png'],
  ['calendar-hero-wide-light.png', 'exec-b7e2360a-0db8-48ab-b768-a381ac5f6a85.png'],
  ['calendar-hero-wide-dark.png', 'exec-1ced59e1-a830-425e-96de-75933d9578b4.png'],
  ['alumni-managers-hero-wide-light.png', 'exec-728048c7-f1d5-498d-969c-6059bccaf750.png'],
  ['alumni-managers-hero-wide-dark.png', 'exec-bce7bd63-6b73-470f-8ed1-73d6ec176712.png'],
]

async function prepareImage(outputName, generatedName) {
  const inputPath = path.join(generatedRoot, generatedName)
  const metadata = await sharp(inputPath).metadata()
  const sourceWidth = metadata.width ?? 0
  const sourceHeight = metadata.height ?? 0
  const cropLeft = Math.round(sourceWidth * 0.25)
  const cropTop = Math.round(sourceHeight * 0.06)
  const isDark = outputName.includes('-dark.')
  const background = isDark
    ? { r: 13, g: 13, b: 15, alpha: 1 }
    : { r: 250, g: 249, b: 247, alpha: 1 }
  const panel = await sharp(inputPath)
    .extract({
      left: cropLeft,
      top: cropTop,
      width: sourceWidth - cropLeft,
      height: sourceHeight - cropTop,
    })
    .resize({ height: 560, fit: 'inside' })
    .ensureAlpha()
    .raw()
    .toBuffer({ resolveWithObject: true })

  for (let index = 0; index < panel.data.length; index += 4) {
    const red = panel.data[index]
    const green = panel.data[index + 1]
    const blue = panel.data[index + 2]
    const maximum = Math.max(red, green, blue)
    const minimum = Math.min(red, green, blue)
    const isNeutral = maximum - minimum <= 10
    const isBackground = isDark ? maximum <= 72 && isNeutral : minimum >= 238 && isNeutral

    if (isBackground) {
      panel.data[index] = background.r
      panel.data[index + 1] = background.g
      panel.data[index + 2] = background.b
    }
  }

  const panelPng = await sharp(panel.data, {
    raw: {
      width: panel.info.width,
      height: panel.info.height,
      channels: 4,
    },
  })
    .png()
    .toBuffer()

  await sharp({
    create: {
      width: 2048,
      height: 560,
      channels: 4,
      background,
    },
  })
    .composite([
      {
        input: panelPng,
        left: 2048 - panel.info.width - 220,
        top: 0,
      },
    ])
    .png({ compressionLevel: 9 })
    .toFile(path.join(outputRoot, outputName))

  console.log(`${outputName}: 2048x560`)
}

Promise.all(images.map(([outputName, generatedName]) => prepareImage(outputName, generatedName))).catch(
  (error) => {
    console.error(error)
    process.exitCode = 1
  },
)
