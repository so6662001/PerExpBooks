export function formatAmount(amount: number): string {
  return `¥${amount.toFixed(2)}`
}

export function formatDate(date: string | Date, format: string = 'YYYY-MM-DD'): string {
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')

  return format
    .replace('YYYY', String(year))
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hours)
    .replace('mm', minutes)
    .replace('ss', seconds)
}

export function formatPhone(phone: string): string {
  if (phone.length !== 11) return phone
  return `${phone.slice(0, 3)}****${phone.slice(7)}`
}

export function getCategoryLabel(category: string): string {
  const map: Record<string, string> = {
    transport: '交通',
    accommodation: '住宿',
    meal: '餐饮',
    office: '办公',
    communication: '通讯',
    subsidy: '补贴',
    other: '其他',
  }
  return map[category] || category
}

export function getCategoryIcon(category: string): string {
  const map: Record<string, string> = {
    transport: '🚗',
    accommodation: '🏨',
    meal: '🍽️',
    office: '📎',
    communication: '📱',
    subsidy: '💰',
    other: '📋',
  }
  return map[category] || '📋'
}

export function getStatusLabel(status: string): string {
  const map: Record<string, string> = {
    pending: '待报销',
    reimbursing: '报销中',
    reimbursed: '已报销',
    generated: '已生成',
    exported: '已导出',
    received: '已收款',
    planned: '计划中',
    ongoing: '进行中',
    completed: '已完成',
    cancelled: '已取消',
  }
  return map[status] || status
}

export function getStatusColor(status: string): string {
  const map: Record<string, string> = {
    pending: '#FF9500',
    reimbursing: '#007AFF',
    reimbursed: '#34C759',
    generated: '#007AFF',
    exported: '#FF9500',
    received: '#34C759',
    planned: '#007AFF',
    ongoing: '#FF9500',
    completed: '#34C759',
    cancelled: '#999999',
  }
  return map[status] || '#999999'
}

export function debounce<T extends (...args: any[]) => any>(fn: T, delay: number): T {
  let timer: ReturnType<typeof setTimeout>
  return ((...args: any[]) => {
    clearTimeout(timer)
    timer = setTimeout(() => fn(...args), delay)
  }) as any
}

export function copyToClipboard(text: string): Promise<void> {
  if (navigator.clipboard) {
    return navigator.clipboard.writeText(text)
  }
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.style.position = 'fixed'
  textarea.style.opacity = '0'
  document.body.appendChild(textarea)
  textarea.select()
  document.execCommand('copy')
  document.body.removeChild(textarea)
  return Promise.resolve()
}
