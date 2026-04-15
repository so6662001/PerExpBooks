<script setup lang="ts">
import { ref, watch, nextTick, computed } from 'vue'
import { showToast } from 'vant'

export interface PosterData {
  type: 'invite' | 'achievement' | 'savings' | 'monthly_report'
  nickname: string
  avatarUrl?: string
  inviteCode: string
  qrCodeUrl: string
  totalExpense?: number
  totalTrips?: number
  totalDays?: number
  savedMinutes?: number
}

const props = defineProps<{
  modelValue: boolean
  posterData: PosterData
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void
}>()

const canvasRef = ref<HTMLCanvasElement>()

const visible = computed({
  get: () => props.modelValue,
  set: (val: boolean) => emit('update:modelValue', val),
})

watch(visible, async (val) => {
  if (val) {
    await nextTick()
    requestAnimationFrame(() => {
      if (canvasRef.value) {
        drawPoster(canvasRef.value, props.posterData)
      }
    })
  }
})

function roundRect(
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  w: number,
  h: number,
  r: number,
) {
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.lineTo(x + w - r, y)
  ctx.quadraticCurveTo(x + w, y, x + w, y + r)
  ctx.lineTo(x + w, y + h - r)
  ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h)
  ctx.lineTo(x + r, y + h)
  ctx.quadraticCurveTo(x, y + h, x, y + h - r)
  ctx.lineTo(x, y + r)
  ctx.quadraticCurveTo(x, y, x + r, y)
  ctx.closePath()
}

function formatMoney(val: number): string {
  return val >= 10000
    ? (val / 10000).toFixed(1).replace(/\.0$/, '') + 'w'
    : val.toLocaleString('zh-CN')
}

function drawInvitePoster(
  ctx: CanvasRenderingContext2D,
  W: number,
  H: number,
  data: PosterData,
) {
  const gradient = ctx.createLinearGradient(0, 0, 0, H)
  gradient.addColorStop(0, '#007AFF')
  gradient.addColorStop(0.6, '#00C6FF')
  gradient.addColorStop(1, '#0072FF')
  ctx.fillStyle = gradient
  ctx.fillRect(0, 0, W, H)

  // Avatar circle
  ctx.save()
  ctx.beginPath()
  ctx.arc(W / 2, 150, 50, 0, Math.PI * 2)
  ctx.fillStyle = 'rgba(255,255,255,0.3)'
  ctx.fill()
  ctx.fillStyle = '#FFFFFF'
  ctx.font = 'bold 36px -apple-system, "Helvetica Neue", sans-serif'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText(data.nickname?.charAt(0) || '钱', W / 2, 150)
  ctx.restore()

  // Nickname + invite text
  ctx.fillStyle = '#FFFFFF'
  ctx.font = '28px -apple-system, "Helvetica Neue", sans-serif'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'alphabetic'
  ctx.fillText(data.nickname + ' 邀请你体验', W / 2, 240)

  // White card
  const cardX = 50
  const cardY = 290
  const cardW = W - 100
  const cardH = 650

  ctx.save()
  ctx.shadowColor = 'rgba(0,0,0,0.15)'
  ctx.shadowBlur = 30
  ctx.shadowOffsetY = 10
  ctx.fillStyle = '#FFFFFF'
  roundRect(ctx, cardX, cardY, cardW, cardH, 24)
  ctx.fill()
  ctx.restore()

  // Card title
  ctx.fillStyle = '#1C1C1E'
  ctx.font = 'bold 48px -apple-system, "Helvetica Neue", sans-serif'
  ctx.textAlign = 'left'
  ctx.fillText('钱酷报销', cardX + 40, cardY + 70)

  ctx.fillStyle = '#8E8E93'
  ctx.font = '26px -apple-system, "Helvetica Neue", sans-serif'
  ctx.fillText('出差报销神器', cardX + 40, cardY + 115)

  // Divider
  ctx.strokeStyle = '#F2F2F7'
  ctx.lineWidth = 1
  ctx.beginPath()
  ctx.moveTo(cardX + 40, cardY + 140)
  ctx.lineTo(cardX + cardW - 40, cardY + 140)
  ctx.stroke()

  // Features
  const features = [
    { icon: '📄', title: '发票PDF拖进去', desc: '自动识别发票信息' },
    { icon: '📋', title: '一键生成报销单', desc: '报销单+发票打包导出' },
    { icon: '💰', title: '出差补贴自动计算', desc: '再也不会忘记报销' },
  ]

  features.forEach((f, i) => {
    const y = cardY + 200 + i * 155

    // Icon background circle
    ctx.save()
    ctx.beginPath()
    ctx.arc(cardX + 70, y - 5, 28, 0, Math.PI * 2)
    ctx.fillStyle = '#F2F2F7'
    ctx.fill()
    ctx.restore()

    ctx.font = '30px sans-serif'
    ctx.textAlign = 'center'
    ctx.fillText(f.icon, cardX + 70, y + 5)

    ctx.textAlign = 'left'
    ctx.fillStyle = '#1C1C1E'
    ctx.font = 'bold 28px -apple-system, "Helvetica Neue", sans-serif'
    ctx.fillText(f.title, cardX + 115, y - 5)

    ctx.fillStyle = '#8E8E93'
    ctx.font = '24px -apple-system, "Helvetica Neue", sans-serif'
    ctx.fillText(f.desc, cardX + 115, y + 30)
  })

  // QR code area
  const qrSize = 180
  const qrX = W / 2 - qrSize / 2
  const qrY = 1020

  ctx.save()
  ctx.shadowColor = 'rgba(0,0,0,0.08)'
  ctx.shadowBlur = 15
  ctx.fillStyle = '#FFFFFF'
  roundRect(ctx, qrX - 15, qrY - 15, qrSize + 30, qrSize + 30, 16)
  ctx.fill()
  ctx.restore()

  ctx.strokeStyle = '#E5E5EA'
  ctx.lineWidth = 1
  roundRect(ctx, qrX - 15, qrY - 15, qrSize + 30, qrSize + 30, 16)
  ctx.stroke()

  // QR placeholder content
  ctx.fillStyle = '#F2F2F7'
  ctx.fillRect(qrX, qrY, qrSize, qrSize)

  ctx.fillStyle = '#8E8E93'
  ctx.font = '22px -apple-system, "Helvetica Neue", sans-serif'
  ctx.textAlign = 'center'
  ctx.fillText('扫码体验', W / 2, qrY + qrSize / 2 + 8)

  // Bottom text
  ctx.fillStyle = '#FFFFFF'
  ctx.font = 'bold 26px -apple-system, "Helvetica Neue", sans-serif'
  ctx.fillText('长按识别 免费体验30天', W / 2, 1265)

  ctx.fillStyle = 'rgba(255,255,255,0.7)'
  ctx.font = '22px -apple-system, "Helvetica Neue", sans-serif'
  ctx.fillText('邀请码: ' + data.inviteCode, W / 2, 1305)
}

function drawAchievementPoster(
  ctx: CanvasRenderingContext2D,
  W: number,
  H: number,
  data: PosterData,
) {
  const gradient = ctx.createLinearGradient(0, 0, 0, H)
  gradient.addColorStop(0, '#FF9500')
  gradient.addColorStop(0.5, '#FF6B00')
  gradient.addColorStop(1, '#FF3B30')
  ctx.fillStyle = gradient
  ctx.fillRect(0, 0, W, H)

  // Decorative circles
  ctx.save()
  ctx.globalAlpha = 0.08
  ctx.fillStyle = '#FFFFFF'
  ctx.beginPath()
  ctx.arc(100, 200, 200, 0, Math.PI * 2)
  ctx.fill()
  ctx.beginPath()
  ctx.arc(650, 400, 150, 0, Math.PI * 2)
  ctx.fill()
  ctx.restore()

  // Title area
  ctx.fillStyle = '#FFFFFF'
  ctx.font = '40px sans-serif'
  ctx.textAlign = 'center'
  ctx.fillText('✈️', W / 2, 140)

  ctx.font = 'bold 44px -apple-system, "Helvetica Neue", sans-serif'
  ctx.fillText('我的出差年报', W / 2, 210)

  ctx.fillStyle = 'rgba(255,255,255,0.8)'
  ctx.font = '24px -apple-system, "Helvetica Neue", sans-serif'
  ctx.fillText(data.nickname + ' 的出差战绩', W / 2, 260)

  // Stats card
  const cardX = 60
  const cardY = 320
  const cardW = W - 120
  const cardH = 560

  ctx.save()
  ctx.shadowColor = 'rgba(0,0,0,0.15)'
  ctx.shadowBlur = 30
  ctx.shadowOffsetY = 10
  ctx.fillStyle = '#FFFFFF'
  roundRect(ctx, cardX, cardY, cardW, cardH, 24)
  ctx.fill()
  ctx.restore()

  const stats = [
    {
      value: String(data.totalDays ?? 0),
      unit: '天',
      label: '出差天数',
      color: '#FF9500',
    },
    {
      value: String(data.totalTrips ?? 0),
      unit: '次',
      label: '出差次数',
      color: '#007AFF',
    },
    {
      value: '¥' + formatMoney(data.totalExpense ?? 0),
      unit: '',
      label: '累计报销',
      color: '#34C759',
    },
    {
      value: String(data.savedMinutes ?? 0),
      unit: '分钟',
      label: '节省时间',
      color: '#5856D6',
    },
  ]

  stats.forEach((s, i) => {
    const y = cardY + 80 + i * 125

    // Color dot
    ctx.beginPath()
    ctx.arc(cardX + 50, y, 8, 0, Math.PI * 2)
    ctx.fillStyle = s.color
    ctx.fill()

    ctx.textAlign = 'left'
    ctx.fillStyle = '#8E8E93'
    ctx.font = '24px -apple-system, "Helvetica Neue", sans-serif'
    ctx.fillText(s.label, cardX + 70, y + 8)

    ctx.textAlign = 'right'
    ctx.fillStyle = '#1C1C1E'
    ctx.font = 'bold 40px -apple-system, "Helvetica Neue", sans-serif'
    ctx.fillText(s.value + s.unit, cardX + cardW - 50, y + 10)
  })

  // Divider in card
  ctx.strokeStyle = '#F2F2F7'
  ctx.lineWidth = 1
  stats.forEach((_, i) => {
    if (i < stats.length - 1) {
      const y = cardY + 80 + i * 125 + 55
      ctx.beginPath()
      ctx.moveTo(cardX + 40, y)
      ctx.lineTo(cardX + cardW - 40, y)
      ctx.stroke()
    }
  })

  // Bottom branding
  ctx.fillStyle = 'rgba(255,255,255,0.6)'
  ctx.font = '24px -apple-system, "Helvetica Neue", sans-serif'
  ctx.textAlign = 'center'
  ctx.fillText('── 钱酷报销 ──', W / 2, 960)

  // QR code area
  const qrSize = 150
  const qrX = W / 2 - qrSize / 2
  const qrY = 1020

  ctx.save()
  ctx.shadowColor = 'rgba(0,0,0,0.08)'
  ctx.shadowBlur = 15
  ctx.fillStyle = '#FFFFFF'
  roundRect(ctx, qrX - 12, qrY - 12, qrSize + 24, qrSize + 24, 14)
  ctx.fill()
  ctx.restore()

  ctx.fillStyle = '#F2F2F7'
  ctx.fillRect(qrX, qrY, qrSize, qrSize)

  ctx.fillStyle = '#8E8E93'
  ctx.font = '20px -apple-system, "Helvetica Neue", sans-serif'
  ctx.textAlign = 'center'
  ctx.fillText('扫码体验', W / 2, qrY + qrSize / 2 + 7)

  ctx.fillStyle = '#FFFFFF'
  ctx.font = 'bold 26px -apple-system, "Helvetica Neue", sans-serif'
  ctx.fillText('扫码体验出差报销神器', W / 2, 1240)

  ctx.fillStyle = 'rgba(255,255,255,0.7)'
  ctx.font = '22px -apple-system, "Helvetica Neue", sans-serif'
  ctx.fillText('邀请码: ' + data.inviteCode, W / 2, 1280)
}

function drawPoster(canvas: HTMLCanvasElement, data: PosterData) {
  const ctx = canvas.getContext('2d')
  if (!ctx) return

  const W = 750
  const H = 1334

  ctx.clearRect(0, 0, W, H)

  if (data.type === 'achievement') {
    drawAchievementPoster(ctx, W, H, data)
  } else {
    drawInvitePoster(ctx, W, H, data)
  }
}

function saveToAlbum() {
  const canvas = canvasRef.value
  if (!canvas) return

  const dataUrl = canvas.toDataURL('image/png')
  const link = document.createElement('a')
  link.download = '钱酷报销_邀请海报.png'
  link.href = dataUrl
  link.click()

  showToast({ message: '海报已保存', type: 'success' })
}
</script>

<template>
  <van-popup
    v-model:show="visible"
    position="center"
    :close-on-click-overlay="false"
    round
    style="width: 90%; max-width: 400px; padding: 0; background: transparent"
  >
    <div class="poster-wrapper">
      <canvas
        ref="canvasRef"
        :width="750"
        :height="1334"
        class="poster-canvas"
      ></canvas>
      <div class="poster-actions">
        <van-button round type="primary" block @click="saveToAlbum">
          保存到相册
        </van-button>
        <van-button round block style="margin-top: 8px" @click="visible = false">
          关闭
        </van-button>
      </div>
    </div>
  </van-popup>
</template>

<style lang="scss" scoped>
.poster-wrapper {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
}

.poster-canvas {
  display: block;
  width: 100%;
  height: auto;
}

.poster-actions {
  padding: 16px;
}
</style>
