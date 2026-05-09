export const tabbarList = [
	{
		title: '首页',
		icon: 'home-line',
		selectedIcon: 'home-fill',
		url: '/pages/home/index',
		openType: 'reLaunch' as const
	},
	{
		title: '消息',
		icon: 'message-2-line',
		selectedIcon: 'message-2-fill',
		url: '/pages/messages/index',
		openType: 'reLaunch' as const
	},
	{
		title: '我的',
		icon: 'user-line',
		selectedIcon: 'user-fill',
		url: '/pages/profile/index',
		openType: 'reLaunch' as const
	}
]
