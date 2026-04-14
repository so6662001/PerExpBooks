const app = getApp()

Page({
  data: {
    inviteCode: '',
    features: [
      { icon: '📸', title: '拍照识票', desc: '手机拍照自动识别发票信息' },
      { icon: '📋', title: '一键报销', desc: '发票拖进去，报销单自动生成' },
      { icon: '📊', title: '费用统计', desc: '清晰掌握每一笔费用支出' }
    ]
  },

  onLoad(options) {
    const code = options.code || ''
    this.setData({ inviteCode: code })

    if (code) {
      app.globalData.inviteCode = code
    }
  },

  handleStart() {
    const code = this.data.inviteCode
    wx.redirectTo({
      url: code
        ? '/pages/index/index?code=' + code
        : '/pages/index/index'
    })
  },

  onShareAppMessage() {
    const code = this.data.inviteCode
    return {
      title: '钱酷报销 - 出差报销神器',
      path: code
        ? '/pages/landing/landing?code=' + code
        : '/pages/landing/landing'
    }
  }
})
