import dayjs from 'dayjs'
import relativeTime from 'dayjs/plugin/relativeTime'
import 'dayjs/locale/en'

dayjs.extend(relativeTime)
dayjs.locale('en')

/**
 * Format time
 * @param time Time string
 * @param format Format string, default is 'YYYY-MM-DD HH:mm:ss'
 * @returns Formatted time string; returns an empty string if time is empty
 */
export const formatTime = (time: string | undefined, format = 'YYYY-MM-DD HH:mm:ss'): string => {
  if (!time) return ''
  return dayjs(time).format(format)
}

/**
 * Format time as relative time
 * @param time Time string
 * @returns Relative time string, such as "2 hours ago"
 */
export const formatRelativeTime = (time: string | undefined): string => {
  if (!time) return ''
  return dayjs(time).fromNow()
}

/**
 * Format time as date
 * @param time Time string
 * @returns Date string, such as "2024-01-01"
 */
export const formatDate = (time: string | undefined): string => {
  if (!time) return ''
  return dayjs(time).format('YYYY-MM-DD')
}
