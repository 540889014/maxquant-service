const { createApp } = Vue;

const BASE_URL = window.location.hostname === 'localhost' ? 'http://localhost:8080' : 'http://123.58.212.96:8080';

createApp({
    data() {
        return {
            isLoggedIn: false,
            username: '',
            password: '',
            loginError: '',
            subscriptions: [],
            statusMessage: '',
            statusMessageColor: 'green',
            unsubscribing: false
        };
    },
    computed: {
        hasOhlcSubscriptions() {
            return this.subscriptions.some(sub => sub.dataType === 'ohlc');
        }
    },
    mounted() {
        const token = localStorage.getItem('token');
        if (token) {
            this.isLoggedIn = true;
            this.username = localStorage.getItem('username') || 'admin';
            this.loadSubscriptions();
        }
    },
    methods: {
        async login() {
            try {
                const response = await axios.post(`${BASE_URL}/api/v1/auth/login`, {
                    username: this.username,
                    password: this.password
                });
                const token = response.data.token;
                localStorage.setItem('token', token);
                localStorage.setItem('username', this.username);
                this.isLoggedIn = true;
                this.loginError = '';
                this.loadSubscriptions();
                console.log('登錄成功:', response.data);
            } catch (error) {
                this.loginError = error.response?.data?.error || '登錄失敗，請檢查用戶名或密碼';
                console.error('登錄錯誤:', error);
            }
        },
        async loadSubscriptions() {
            try {
                const token = localStorage.getItem('token');
                if (!token) {
                    throw new Error('未找到 token，請重新登錄');
                }
                const response = await axios.get(`${BASE_URL}/api/v1/subscription/user?username=${this.username}`, {
                    headers: { Authorization: `Bearer ${token}` }
                });
                this.subscriptions = response.data || [];
                console.log('已訂閱契約:', this.subscriptions);
            } catch (error) {
                console.error('加載訂閱失敗:', error.response || error);
                if (error.response && error.response.status === 403) {
                    this.statusMessage = '權限不足，請重新登錄';
                    this.statusMessageColor = 'red';
                    this.isLoggedIn = false;
                } else {
                    this.statusMessage = '加載訂閱失敗，請重試';
                    this.statusMessageColor = 'red';
                }
            }
        },
        async unsubscribe(sub) {
            this.unsubscribing = true;
            try {
                const token = localStorage.getItem('token');
                if (!token) {
                    throw new Error('未找到 token，請重新登錄');
                }
                const params = {
                    username: sub.username,
                    symbol: sub.symbol,
                    dataType: sub.dataType
                };
                console.log('取消訂閱請求參數:', params);
                const response = await axios.post(`${BASE_URL}/api/v1/subscription/unsubscribe`, null, {
                    params: params,
                    headers: { Authorization: `Bearer ${token}` }
                });
                console.log('取消訂閱成功:', response.data);
                this.statusMessage = `已取消訂閱 ${sub.symbol} 的 ${sub.dataType} 數據`;
                this.statusMessageColor = 'green';
                this.loadSubscriptions();
            } catch (error) {
                console.error('取消訂閱失敗:', error.response || error);
                if (error.response && error.response.status === 403) {
                    this.statusMessage = '權限不足，請重新登錄';
                    this.statusMessageColor = 'red';
                    this.isLoggedIn = false;
                } else {
                    this.statusMessage = error.response?.data?.message || '取消訂閱失敗，請重試';
                    this.statusMessageColor = 'red';
                }
            } finally {
                this.unsubscribing = false;
            }
        }
    }
}).mount('#app');