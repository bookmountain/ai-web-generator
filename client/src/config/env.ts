/**
 * Environment configuration
 */

// Application deployment domain
export const DEPLOY_DOMAIN = import.meta.env.VITE_DEPLOY_DOMAIN || 'http://localhost'

// API base URL
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8123/api'

// Static resource URL
export const STATIC_BASE_URL = `${API_BASE_URL}/static`

// Get the full URL of the deployed app
export const getDeployUrl = (deployKey: string) => {
  return `${DEPLOY_DOMAIN}/${deployKey}`
}

// Get static resource preview URL
export const getStaticPreviewUrl = (codeGenType: string, appId: string) => {
  return `${STATIC_BASE_URL}/${codeGenType}_${appId}/`
}
