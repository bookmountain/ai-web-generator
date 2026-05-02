import { useLoginUserStore } from '@/stores/loginUser'
import { message } from 'ant-design-vue'
import router from '@/router'

// Whether this is the first fetch of the logged-in user
let firstFetchLoginUser = true

/**
 * Global permission check
 */
router.beforeEach(async (to, from, next) => {
  const loginUserStore = useLoginUserStore()
  let loginUser = loginUserStore.loginUser
  // Ensure that after a page refresh, the first load waits for the backend user info before checking permissions
  if (firstFetchLoginUser) {
    await loginUserStore.fetchLoginUser()
    loginUser = loginUserStore.loginUser
    firstFetchLoginUser = false
  }
  const toUrl = to.fullPath
  if (toUrl.startsWith('/admin')) {
    if (!loginUser || loginUser.userRole !== 'admin') {
      message.error('No permission')
      next(`/user/login?redirect=${to.fullPath}`)
      return
    }
  }
  next()
})
