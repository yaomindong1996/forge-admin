import { sm4 } from 'sm-crypto'

export function sm4Encrypt(plainText: string, key: string): string {
  if (!plainText) return plainText
  const hexCipherText = sm4.encrypt(plainText, key)
  return hexToBase64(hexCipherText)
}

export function sm4Decrypt(cipherText: string, key: string): string {
  if (!cipherText) return cipherText
  const hexCipherText = base64ToHex(cipherText)
  return sm4.decrypt(hexCipherText, key)
}

function hexToBase64(hexString: string): string {
  const bytes = []
  for (let i = 0; i < hexString.length; i += 2) {
    bytes.push(Number.parseInt(hexString.substr(i, 2), 16))
  }
  return btoa(String.fromCharCode.apply(null, bytes))
}

function base64ToHex(base64String: string): string {
  const raw = atob(base64String)
  let result = ''
  for (let i = 0; i < raw.length; i++) {
    const hex = raw.charCodeAt(i).toString(16)
    result += hex.length === 2 ? hex : `0${hex}`
  }
  return result
}

export function decodeKeyToHex(base64Key: string): string {
  return base64ToHex(base64Key)
}
