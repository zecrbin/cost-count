import { notifications } from '@mantine/notifications';

export function notifySuccess(message) {
  notifications.show({ color: 'teal', message, autoClose: 2500 });
}

export function notifyError(error, title = '操作失败') {
  notifications.show({ color: 'red', title, message: error?.message || String(error), autoClose: 5000 });
}
