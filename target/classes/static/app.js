const { createApp } = Vue;

// 配置基础 URL，优先使用 localhost
const BASE_URL = window.location.hostname === 'localhost' ? 'http://localhost:8080' : 'http://123.58.212.96:8080';

createApp({
    data() {
        return {
            isLoggedIn: false,
            username: '',
            password: '',
            loginError: '',
            instruments: [],
            filteredInstruments: [],
            searchQuery: '',
            selectedSymbol: '',
            selectedInstType: 'SPOT',
            dataType: 'realtime',
            timeframe: '1h',
            displayData: [],
            depthData: { bids: [], asks: [] },
            ws: null,
            chart: null,
            subscriptionStatus: '',
            hasSubscription: false, // 跟踪是否已訂閱
            isAdmin: false, // 跟踪是否为管理员
            selectedSymbol2: '',
            spreadData: []
        };
    },
    mounted() {
        this.checkTokenValidity();
    },
    methods: {
        async checkTokenValidity() {
            const token = localStorage.getItem('token');
            if (token) {
                try {
                    await axios.get(`${BASE_URL}/api/v1/auth/validate`, {
                        headers: { Authorization: `Bearer ${token}` }
                    });
                    this.isLoggedIn = true;
                    this.username = localStorage.getItem('username') || 'admin';
                    this.checkAdminStatus();
                    this.loadInstruments();
                    this.connectWebSocket();
                } catch (error) {
                    console.error('Token 失效:', error);
                    localStorage.removeItem('token');
                    localStorage.removeItem('username');
                    localStorage.removeItem('currentUser');
                    this.isLoggedIn = false;
                    this.isAdmin = false;
                }
            } else {
                this.isLoggedIn = false;
            }
        },
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
                this.checkAdminStatus();
                this.loadInstruments();
                this.connectWebSocket();
                console.log('登錄成功:', response.data);
            } catch (error) {
                this.loginError = error.response?.data?.error || '登錄失敗，請檢查用戶名或密碼';
                console.error('登錄錯誤:', error);
            }
        },
        checkAdminStatus() {
            // 假设从后端获取当前用户信息，包括角色
            const token = localStorage.getItem('token');
            if (token) {
                axios.get(`${BASE_URL}/api/users/all`, {
                    headers: { Authorization: `Bearer ${token}` }
                })
                .then(response => {
                    const currentUser = response.data.find(user => user.username === this.username);
                    if (currentUser && currentUser.role === 'ADMIN') {
                        this.isAdmin = true;
                        localStorage.setItem('currentUser', JSON.stringify(currentUser));
                    } else {
                        this.isAdmin = false;
                        localStorage.removeItem('currentUser');
                    }
                })
                .catch(error => {
                    console.error('获取用户信息失败:', error);
                    this.isAdmin = false;
                });
            }
        },
        async loadInstruments() {
            try {
                console.log('開始加載交易對，instType:', this.selectedInstType);
                const response = await axios.get(`${BASE_URL}/api/v5/public/instruments?instType=${this.selectedInstType}`); // 移除 headers
                console.log('交易對響應:', response.data);
                this.instruments = response.data || [];
                this.filterInstruments();
                if (this.filteredInstruments.length > 0) {
                    this.selectedSymbol = this.filteredInstruments[0].instId;
                    console.log('交易對加載成功，選擇第一個:', this.selectedSymbol);
                    this.loadData();
                    await this.checkSubscription();
                } else {
                    console.warn('交易對數據為空');
                    this.selectedSymbol = '';
                }
            } catch (error) {
                console.error('交易對加載失敗:', error.response || error);
            }
        },
        filterInstruments() {
            if (!this.searchQuery) {
                this.filteredInstruments = this.instruments;
                console.log('未輸入搜索，顯示所有交易對:', this.filteredInstruments.length);
                return;
            }
            const query = this.searchQuery.toLowerCase();
            this.filteredInstruments = this.instruments.filter(inst =>
                inst.instId.toLowerCase().startsWith(query)
            );
            console.log('搜索交易對:', query, '結果:', this.filteredInstruments);
            if (!this.filteredInstruments.some(inst => inst.instId === this.selectedSymbol)) {
                this.selectedSymbol = this.filteredInstruments.length > 0 ? this.filteredInstruments[0].instId : '';
                console.log('更新選擇的交易對:', this.selectedSymbol);
            }
        },
        async checkSubscription() {
            if (!this.selectedSymbol || !this.dataType) {
                this.hasSubscription = false;
                return;
            }
            try {
                const response = await axios.get(`${BASE_URL}/api/v1/subscription/user?username=${this.username}`, {
                    headers: { Authorization: `Bearer ${localStorage.getItem('token')}` }
                });
                const subscriptions = response.data || [];
                this.hasSubscription = subscriptions.some(sub =>
                    sub.symbol === this.selectedSymbol && sub.dataType === this.dataType
                );
                console.log('檢查訂閱狀態:', this.hasSubscription);
            } catch (error) {
                console.error('檢查訂閱狀態失敗:', error.response || error);
                this.hasSubscription = false;
            }
        },
        async subscribeMarket() {
            if (!this.selectedSymbol || !this.dataType) {
                console.warn('請選擇合約和數據類型');
                return;
            }
            const token = localStorage.getItem('token');
            if (!token) {
                console.error('未找到 token，請重新登錄');
                this.subscriptionStatus = '請重新登錄';
                this.isLoggedIn = false;
                return;
            }
            try {
                const params = {
                    username: this.username,
                    symbol: this.selectedSymbol,
                    dataType: this.dataType,
                    instType: this.selectedInstType
                };
                if (this.dataType === 'ohlc') {
                    params.timeframe = this.timeframe;
                }
                console.log('訂閱請求參數:', params);
                const response = await axios.post(`${BASE_URL}/api/v1/subscription/subscribe`, null, {
                    params: params,
                    headers: { Authorization: `Bearer ${token}` }
                });
                console.log('訂閱成功:', response.data);
                this.subscriptionStatus = `已訂閱 ${this.selectedSymbol} 的 ${this.dataType}${this.dataType === 'ohlc' ? '（' + this.timeframe + '）' : ''} 數據`;
                this.hasSubscription = true;
            } catch (error) {
                console.error('訂閱失敗:', error.response || error);
                if (error.response && error.response.status === 403) {
                    this.subscriptionStatus = '權限不足，請重新登錄';
                    this.isLoggedIn = false;
                } else {
                    this.subscriptionStatus = error.response?.data?.message || '訂閱失敗，請重試';
                }
            }
        },
        async unsubscribeMarket() {
            if (!this.selectedSymbol || !this.dataType) {
                console.warn('請選擇合約和數據類型');
                return;
            }
            const token = localStorage.getItem('token');
            if (!token) {
                console.error('未找到 token，請重新登錄');
                this.subscriptionStatus = '請重新登錄';
                this.isLoggedIn = false;
                return;
            }
            try {
                const params = {
                    username: this.username,
                    symbol: this.selectedSymbol,
                    dataType: this.dataType
                };
                console.log('取消訂閱請求參數:', params);
                const response = await axios.post(`${BASE_URL}/api/v1/subscription/unsubscribe`, null, {
                    params: params,
                    headers: { Authorization: `Bearer ${token}` }
                });
                console.log('取消訂閱成功:', response.data);
                this.subscriptionStatus = `已取消訂閱 ${this.selectedSymbol} 的 ${this.dataType} 數據`;
                this.hasSubscription = false;
            } catch (error) {
                console.error('取消訂閱失敗:', error.response || error);
                if (error.response && error.response.status === 403) {
                    this.subscriptionStatus = '權限不足，請重新登錄';
                    this.isLoggedIn = false;
                } else {
                    this.subscriptionStatus = error.response?.data?.message || '取消訂閱失敗，請重試';
                }
            }
        },
        async loadData() {
            if (!this.selectedSymbol) {
                console.warn('未選擇交易對，無法加載數據');
                return;
            }
            this.displayData = [];
            this.depthData = { bids: [], asks: [] };
            this.spreadData = [];
            try {
                const token = localStorage.getItem('token');
                if (!token) {
                    throw new Error('未找到 token，請重新登錄');
                }
                if (this.dataType === 'realtime') {
                    console.log('實時數據請求已移除');
                    this.displayData = [];
                } else if (this.dataType === 'ohlc') {
                    const endTime = Date.now();
                    const startTime = endTime - 24 * 60 * 60 * 1000;
                    const url = `${BASE_URL}/api/v1/market/kline?symbol=${encodeURIComponent(this.selectedSymbol)}&timeframe=${this.timeframe}&startTime=${startTime}&endTime=${endTime}`;
                    console.log('獲取 OHLC 數據，URL:', url);
                    const response = await axios.get(url, {
                        headers: { Authorization: `Bearer ${token}` }
                    });
                    console.log('OHLC 數據響應:', response.data);
                    this.displayData = response.data || [];
                    console.log('更新 displayData:', this.displayData);
                    this.updateChart();
                } else if (this.dataType === 'depth') {
                    const url = `${BASE_URL}/api/v1/market/depth?symbol=${encodeURIComponent(this.selectedSymbol)}`;
                    console.log('獲取深度數據，URL:', url);
                    const response = await axios.get(url, {
                        headers: { Authorization: `Bearer ${token}` }
                    });
                    console.log('深度數據響應:', response.data);
                    if (response.data) {
                        this.depthData = {
                            bids: JSON.parse(response.data.bids),
                            asks: JSON.parse(response.data.asks)
                        };
                    }
                } else if (this.dataType === 'spread') {
                    if (!this.selectedSymbol2) {
                        console.warn('请选择第二个合约');
                        return;
                    }
                    const endTime = Date.now();
                    const startTime = endTime - 24 * 60 * 60 * 1000;
                    const url1 = `${BASE_URL}/api/v1/market/kline?symbol=${encodeURIComponent(this.selectedSymbol)}&timeframe=${this.timeframe}&startTime=${startTime}&endTime=${endTime}`;
                    const url2 = `${BASE_URL}/api/v1/market/kline?symbol=${encodeURIComponent(this.selectedSymbol2)}&timeframe=${this.timeframe}&startTime=${startTime}&endTime=${endTime}`;
                    const [resp1, resp2] = await Promise.all([
                        axios.get(url1, { headers: { Authorization: `Bearer ${token}` } }),
                        axios.get(url2, { headers: { Authorization: `Bearer ${token}` } })
                    ]);
                    const data1 = resp1.data || [];
                    const data2 = resp2.data || [];
                    // 按时间戳对齐，计算收盘价差
                    const map2 = new Map(data2.map(item => [item.timestamp, item]));
                    this.spreadData = data1
                        .filter(item => map2.has(item.timestamp))
                        .map(item => ({
                            x: new Date(item.timestamp).getTime(),
                            y: parseFloat(item.closePrice) - parseFloat(map2.get(item.timestamp).closePrice)
                        }));
                    this.updateSpreadChart();
                }
            } catch (error) {
                console.error('數據加載失敗:', error.response || error);
                if (error.response && error.response.status === 403) {
                    this.subscriptionStatus = '權限不足，請重新登錄';
                    this.isLoggedIn = false;
                } else {
                    this.subscriptionStatus = error.response?.data?.message || '數據加載失敗，請重試';
                }
            }
        },
        connectWebSocket() {
            this.ws = new WebSocket(BASE_URL.replace('http', 'ws') + '/ws/market');
            this.ws.onmessage = (event) => {
                console.log('WebSocket 消息:', event.data);
                const data = JSON.parse(event.data);
                if (data.symbol === this.selectedSymbol) {
                    if (data.type === 'realtime' && this.dataType === 'realtime') {
                        this.displayData = [{ symbol: data.symbol, price: data.price, timestamp: data.timestamp }];
                    } else if (data.type === 'depth' && this.dataType === 'depth') {
                        this.depthData = {
                            bids: JSON.parse(data.bids),
                            asks: JSON.parse(data.asks)
                        };
                    }
                }
            };
            this.ws.onerror = (error) => {
                console.error('WebSocket 錯誤:', error);
            };
            this.ws.onclose = () => {
                console.log('WebSocket 關閉，5秒後重連');
                setTimeout(() => this.connectWebSocket(), 5000);
            };
        },
        updateChart() {
            if (this.dataType !== 'ohlc') return;
            const ctx = document.getElementById('klineChart');
            if (this.chart && this.chart.destroy) {
                this.chart.destroy();
            }
            if (this.displayData.length === 0) {
                console.log('無數據可繪製');
                return;
            }
            const seriesData = this.displayData.map(item => ({
                x: new Date(item.timestamp).getTime(),
                y: [
                    parseFloat(item.openPrice) || 0,
                    parseFloat(item.highPrice) || 0,
                    parseFloat(item.lowPrice) || 0,
                    parseFloat(item.closePrice) || 0
                ]
            }));
            console.log('ApexCharts 數據：', seriesData);
            this.chart = new ApexCharts(ctx, {
                series: [{
                    data: seriesData
                }],
                chart: {
                    type: 'candlestick',
                    height: 350
                },
                title: {
                    text: this.selectedSymbol + ' K線圖',
                    align: 'left'
                },
                xaxis: {
                    type: 'datetime'
                },
                yaxis: {
                    tooltip: {
                        enabled: true
                    }
                }
            });
            this.chart.render();
        },
        updateSpreadChart() {
            const ctx = document.getElementById('spreadChart');
            ctx.innerHTML = '';
            if (this.spreadChart && this.spreadChart.destroy) {
                this.spreadChart.destroy();
            }
            if (!this.spreadData || this.spreadData.length === 0) {
                console.log('无价差数据可绘制');
                return;
            }
            this.spreadChart = new ApexCharts(ctx, {
                series: [{
                    name: '价差',
                    data: this.spreadData
                }],
                chart: {
                    type: 'line',
                    height: 350
                },
                title: {
                    text: `${this.selectedSymbol} - ${this.selectedSymbol2} 价差曲线`,
                    align: 'left'
                },
                xaxis: {
                    type: 'datetime'
                },
                yaxis: {
                    tooltip: {
                        enabled: true
                    }
                }
            });
            this.spreadChart.render();
        }
    }
}).mount('#app');