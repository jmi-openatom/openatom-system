declare const QRCode: {
  create(
    text: string,
    options?: Record<string, unknown>,
  ): {
    modules: {
      size: number
      data: Uint8Array
    }
  }
}

export default QRCode
