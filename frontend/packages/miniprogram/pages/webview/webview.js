const config = require('../../utils/config')
const app = getApp()

Page({
  data: {
    url: ''
  },

  onLoad(options) {
    if (!options.url) {
      wx.navigateBack()
      return
    }

    let targetUrl = decodeURIComponent(options.url)

    if (!targetUrl.startsWith('http')) {
      targetUrl = config.H5_BASE_URL + targetUrl
    }

    this.setData({ url: targetUrl })
  },

  onMessage(e) {
    const data = e.detail.data
    if (!data || !data.length) return

    const message = data[data.length - 1]
    console.log('H5 message:', message)

    if (message.action === 'navigate') {
      wx.navigateTo({
        url: '/pages/webview/webview?url=' + encodeURIComponent(message.url)
      })
    } else if (message.action === 'back') {
      wx.navigateBack()
    }
  },

  onShareAppMessage() {
    const inviteCode = app.globalData.inviteCode
    return {
      title: '钱酷报销 - 出差报销神器',
      path: inviteCode
        ? '/pages/landing/landing?code=' + inviteCode
        : '/pages/index/index'
    }
  },

  onShareTimeline() {
    const inviteCode = app.globalData.inviteCode
    return {
      title: '钱酷报销 - 出差报销神器',
      query: inviteCode ? 'code=' + inviteCode : ''
    }
  }
})
