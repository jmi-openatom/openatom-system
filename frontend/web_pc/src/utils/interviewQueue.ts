import { ElMessageBox } from 'element-plus/es/components/message-box/index'

export async function promptForceCallReason(candidateName: string, roomName: string): Promise<string | null> {
  try {
    const { value } = await ElMessageBox.prompt(
      `将在“${roomName}”跳过“${candidateName}”的评价等待，呼叫下一位已签到候选人。已有评价会保留，未完成的评价可稍后补交。请填写原因。`,
      '强制叫号',
      {
        type: 'warning',
        inputType: 'textarea',
        inputPlaceholder: '例如：面试官临时离场，稍后补交评价',
        inputValidator: (value) => {
          const reason = String(value || '').trim()
          if (!reason) return '请填写强制叫号原因'
          return reason.length <= 500 || '原因不能超过 500 字'
        },
        confirmButtonText: '确认强制叫下一位',
        cancelButtonText: '取消',
        closeOnClickModal: false,
      },
    )
    return value.trim()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return null
    throw error
  }
}
