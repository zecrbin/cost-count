import { notifications } from '@mantine/notifications';

export function notifySuccess(message, emoji = '✨') {
  notifications.show({ color: 'teal', message: `${message} ${emoji}`, autoClose: 2500 });
}

export function notifyError(error, title = '哎呀，出错了') {
  notifications.show({ color: 'red', title: `${title} 😣`, message: error?.message || String(error), autoClose: 5000 });
}
