import CryptoJS from 'crypto-js'
import JSEncrypt from 'jsencrypt-ext'

/**
 * 支持 RSA/AES/DES 加密解密
 */
export class EncryptTool {
  static KEYS = {
    // aes加密key
    AES: '2dzfLH3NcbWIVKX67MGqOg==',
    // des加密key
    DES: '5luRXs4qlNzCtYYg37yoGeZbkV7OKpTc',
    // rsa加密的私钥
    RSA_PRIVATE_KEY: 'MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAIsNNf0dtHj789o/pJ/MGW5P/ExoXGMA9SgFA35bR4tC9pmyxBbSZsbVB0cM7x3u87gcCajTUxYn3IKXCTgEE451Ii1X9w+HHbrbMmxqHy4yRhfvtz1Wnax4H52OEmbqvOMNHCEcOu2VMjTNcYWphx5rI+jFgEECvoijqquyFUMhAgMBAAECgYAZKto1dqmqRyHvA9tiP9DvhiMaWj40fiEhLe0gstk7Y7KaBYDXx5uqC7cePUFPN+ddUJfP+nW8mhjfewJEZPp75zeaQ4ZT1rvrbTddNP86wIxR6B21kWcN+qzl36gqjs/YD06FX4G6+Tkpr9c++AVVnX0N7eDZsjUeUSD4viMEtQJBAMDRVTF0wVNQCxM5js8xa9m79GoQmeLsoPn8zy8+UsfK9Xcos17z5KuCAxNF4f+upVgiB2W1aTB70GWQPdFx5e0CQQC4napxRFoafjaDtRxWEULzxvaQE/J88/e+4nE17KEApeMB4A4SPlUSPOObFUSvq+zAhzXPSiMoSPTUIok4MiuFAkEAucY2peLPDV02fnxbSudb+TmtQK/kBI5mftWELQmIKnUy0W8GezrTnCc0nVIccAMtzV9j7yu9QHhajjBOT0jE9QJBAIyJ6dBj+jukzRcibjnO1isB0X0I0lXw2y6C7y/fvQjjP5aT2+JjdRt3IsVSQYGB3a8KvxnYFD98mnRK/i67cokCQB1+M6li6M6XsovPF6iICTuK4BA1mFVMKoBvEvPYw+/mxDiHs8b8ARI9EL+Y4Oxf6vTrQbZ4ZZPEkPJSTM4G+RQ=',
    // rsa加密的公钥
    RSA_PUBLIC_KEY: 'MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCLDTX9HbR4+/PaP6SfzBluT/xMaFxjAPUoBQN+W0eLQvaZssQW0mbG1QdHDO8d7vO4HAmo01MWJ9yClwk4BBOOdSItV/cPhx262zJsah8uMkYX77c9Vp2seB+djhJm6rzjDRwhHDrtlTI0zXGFqYceayPoxYBBAr6Io6qrshVDIQIDAQAB',
  }

  /**
   * RSA加密
   * @param encryptStr 需要加密的内容
   */
  static rsaEncrypt(encryptStr) {
    const encrypt = new JSEncrypt()
    encrypt.setPublicKey(EncryptTool.KEYS.RSA_PUBLIC_KEY)
    return encrypt.encrypt(encryptStr)
  }

  /**
   * RSA解密
   * @param decryptStr 需要解密的内容
   */
  static rsaDecrypt(decryptStr) {
    const decrypt = new JSEncrypt()
    decrypt.setPublicKey(EncryptTool.KEYS.RSA_PRIVATE_KEY)
    return decrypt.decrypt(decryptStr)
  }

  /**
   * AES/DES加密
   * @param encryptStr 需要加密的内容
   * @param mode 加密模式 AES | DES
   */
  static aesOrDesEncrypt(encryptStr, mode) {
    return CryptoJS[mode].encrypt(encryptStr, EncryptTool.KEYS[mode]).toString()
  }

  /**
   * AES/DES解密
   * @param decryptStr 需要解密的内容
   * @param mode 解密模式 AES | DES
   */
  static aesOrDesDecrypt(decryptStr, mode) {
    const bytes = CryptoJS[mode].decrypt(decryptStr, EncryptTool.KEYS[mode])
    return bytes.toString(CryptoJS.enc.Utf8)
  }

  /**
   * 加密
   */
  static encrypt(mode, encryptStr) {
    if (mode && encryptStr) {
      const modeMap = {
        RSA: () => EncryptTool.rsaEncrypt(encryptStr),
        AES: () => EncryptTool.aesOrDesEncrypt(encryptStr, 'AES'),
        DES: () => EncryptTool.aesOrDesEncrypt(encryptStr, 'DES'),
      }
      if (modeMap[mode]) {
        return modeMap[mode]()
      }
    }
    return ''
  }

  /**
   * 解密
   */
  static decrypt(mode, decryptStr) {
    if (mode && decryptStr) {
      const modeMap = {
        RSA: () => EncryptTool.rsaDecrypt(decryptStr),
        AES: () => EncryptTool.aesOrDesDecrypt(decryptStr, 'AES'),
        DES: () => EncryptTool.aesOrDesDecrypt(decryptStr, 'DES'),
      }
      if (modeMap[mode]) {
        return modeMap[mode]()
      }
    }
    return ''
  }
}
