export const SIMPLE_SCHEDULE_TYPES = [
  { label: '每隔几分钟', value: 'interval' },
  { label: '每小时', value: 'hourly' },
  { label: '每天', value: 'daily' },
  { label: '每周', value: 'weekly' },
  { label: '每月', value: 'monthly' },
]

export const WEEKDAY_OPTIONS = [
  { label: '周一', value: 'MON' },
  { label: '周二', value: 'TUE' },
  { label: '周三', value: 'WED' },
  { label: '周四', value: 'THU' },
  { label: '周五', value: 'FRI' },
  { label: '周六', value: 'SAT' },
  { label: '周日', value: 'SUN' },
]

export function createDefaultSimpleSchedule() {
  return {
    type: 'daily',
    intervalMinutes: 10,
    minute: 0,
    time: '02:00',
    weekday: 'MON',
    dayOfMonth: 1,
  }
}

export function buildCronExpression(schedule) {
  const data = { ...createDefaultSimpleSchedule(), ...schedule }
  const [hour, minute] = parseTime(data.time)
  if (data.type === 'interval')
    return `0 0/${clampInteger(data.intervalMinutes, 1, 59)} * * * ?`
  if (data.type === 'hourly')
    return `0 ${clampInteger(data.minute, 0, 59)} * * * ?`
  if (data.type === 'weekly')
    return `0 ${minute} ${hour} ? * ${normalizeWeekday(data.weekday)}`
  if (data.type === 'monthly')
    return `0 ${minute} ${hour} ${clampInteger(data.dayOfMonth, 1, 31)} * ?`
  return `0 ${minute} ${hour} * * ?`
}

export function parseCronExpression(expression) {
  const cron = String(expression || '').trim().replace(/\s+/g, ' ')
  let match = cron.match(/^0 (?:0|\*)\/(\d{1,2}) \* \* \* \?$/)
  if (match && inRange(match[1], 1, 59))
    return { ...createDefaultSimpleSchedule(), type: 'interval', intervalMinutes: Number(match[1]) }

  match = cron.match(/^0 (\d{1,2}) \* \* \* \?$/)
  if (match && inRange(match[1], 0, 59))
    return { ...createDefaultSimpleSchedule(), type: 'hourly', minute: Number(match[1]) }

  match = cron.match(/^0 (\d{1,2}) (\d{1,2}) \* \* \?$/)
  if (match && validTime(match[2], match[1]))
    return { ...createDefaultSimpleSchedule(), type: 'daily', time: formatTime(match[2], match[1]) }

  match = cron.match(/^0 (\d{1,2}) (\d{1,2}) \? \* (MON|TUE|WED|THU|FRI|SAT|SUN)$/i)
  if (match && validTime(match[2], match[1])) {
    return {
      ...createDefaultSimpleSchedule(),
      type: 'weekly',
      time: formatTime(match[2], match[1]),
      weekday: match[3].toUpperCase(),
    }
  }

  match = cron.match(/^0 (\d{1,2}) (\d{1,2}) (\d{1,2}) \* \?$/)
  if (match && validTime(match[2], match[1]) && inRange(match[3], 1, 31)) {
    return {
      ...createDefaultSimpleSchedule(),
      type: 'monthly',
      time: formatTime(match[2], match[1]),
      dayOfMonth: Number(match[3]),
    }
  }
  return null
}

function parseTime(value) {
  const match = String(value || '').match(/^(\d{1,2}):(\d{1,2})$/)
  if (!match || !validTime(match[1], match[2]))
    return [2, 0]
  return [Number(match[1]), Number(match[2])]
}

function validTime(hour, minute) {
  return inRange(hour, 0, 23) && inRange(minute, 0, 59)
}

function formatTime(hour, minute) {
  return `${String(Number(hour)).padStart(2, '0')}:${String(Number(minute)).padStart(2, '0')}`
}

function inRange(value, min, max) {
  const number = Number(value)
  return Number.isInteger(number) && number >= min && number <= max
}

function clampInteger(value, min, max) {
  const number = Number.parseInt(value, 10)
  if (Number.isNaN(number))
    return min
  return Math.min(max, Math.max(min, number))
}

function normalizeWeekday(value) {
  const weekday = String(value || '').toUpperCase()
  return WEEKDAY_OPTIONS.some(item => item.value === weekday) ? weekday : 'MON'
}
