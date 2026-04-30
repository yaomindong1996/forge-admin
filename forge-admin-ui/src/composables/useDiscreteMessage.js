import { createDiscreteApi } from 'naive-ui'

const { message } = createDiscreteApi(['message'])

export function useDiscreteMessage() {
  return {
    success: content => message.success(content),
    error: content => message.error(content),
    warning: content => message.warning(content),
    info: content => message.info(content),
    loading: content => message.loading(content),
  }
}

export default message
