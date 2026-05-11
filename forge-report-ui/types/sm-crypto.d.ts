declare module 'sm-crypto' {
  export const sm4: {
    encrypt(plainText: string, key: string): string
    decrypt(cipherText: string, key: string): string
  }
}
