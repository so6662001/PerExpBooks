const config = require('./utils/config')

App({
  globalData: {
    inviteCode: '',
    scene: 0
  },

  onLaunch(options) {
    this.globalData.scene = options.scene || 0

    if (options.query && options.query.code) {
      this.globalData.inviteCode = options.query.code
    }

    this.checkLoginStatus()
  },

  checkLoginStatus() {
    wx.checkSession({
      fail() {
        wx.login({
          success(res) {
            if (res.code) {
              console.log('login code:', res.code)
            }
          }
        })
      }
    })
  },

  onShareAppMessage() {
    const inviteCode = this.globalData.inviteCode
    return {
      title: '钱酷报销 - 出差报销神器',
      path: inviteCode
        ? '/pages/landing/landing?code=' + inviteCode
        : '/pages/index/index'
    }
  }
})
