const config = require('../../utils/config')
const app = getApp()

Page({
  data: {
    url: ''
  },

  onLoad(options) {
    let url = config.H5_BASE_URL
    const inviteCode = options.code || app.globalData.inviteCode

    if (inviteCode) {
      url += '?inviteCode=' + inviteCode
      app.globalData.inviteCode = inviteCode
    }

    this.setData({ url })
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
