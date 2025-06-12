const BASE_URL = window.location.hostname === 'localhost' ? 'http://localhost:8080' : 'http://123.58.212.96:8080';

const app = Vue.createApp({
    data() {
        return {
            input1: '',
            input2: '',
            selected1: null,
            selected2: null,
            showDropdown1: false,
            showDropdown2: false,
            allInstruments: [],
            filtered1: [],
            filtered2: [],
            dataType: 'ohlc',
            timeframe: '1h',
            kline1: [],
            kline2: [],
            spread: [],
            chart: null,
            loading: false,
            error: ''
        };
    },
    mounted() {
        this.fetchInstruments();
        document.addEventListener('click', this.handleClickOutside);
    },
    beforeUnmount() {
        document.removeEventListener('click', this.handleClickOutside);
    },
    methods: {
        async fetchInstruments() {
            try {
                const res = await axios.get(`${BASE_URL}/api/v5/public/instruments?instType=SWAP`);
                this.allInstruments = res.data || [];
                this.filtered1 = this.allInstruments;
                this.filtered2 = this.allInstruments;
            } catch (e) {
                this.error = '合约列表获取失败';
            }
        },
        onInput1() {
            this.showDropdown1 = true;
            const q = this.input1.trim().toLowerCase();
            this.filtered1 = this.allInstruments.filter(inst => inst.instId.toLowerCase().includes(q));
        },
        onInput2() {
            this.showDropdown2 = true;
            const q = this.input2.trim().toLowerCase();
            this.filtered2 = this.allInstruments.filter(inst => inst.instId.toLowerCase().includes(q));
        },
        select1(item) {
            this.selected1 = item;
            this.input1 = item.instId;
            this.showDropdown1 = false;
        },
        select2(item) {
            this.selected2 = item;
            this.input2 = item.instId;
            this.showDropdown2 = false;
        },
        handleClickOutside(e) {
            if (!e.target.closest('.input-group')) {
                this.showDropdown1 = false;
                this.showDropdown2 = false;
            }
        },
        async calculateSpread() {
            if (!this.selected1 || !this.selected2) return;
            this.loading = true;
            this.error = '';
            this.kline1 = [];
            this.kline2 = [];
            this.spread = [];
            if (this.chart && this.chart.destroy) this.chart.destroy();
            try {
                if (this.dataType === 'ohlc') {
                    const endTime = Date.now();
                    const startTime = endTime - 24 * 60 * 60 * 1000;
                    const url1 = `${BASE_URL}/api/v1/market/kline?symbol=${encodeURIComponent(this.selected1.instId)}&timeframe=${this.timeframe}&startTime=${startTime}&endTime=${endTime}`;
                    const url2 = `${BASE_URL}/api/v1/market/kline?symbol=${encodeURIComponent(this.selected2.instId)}&timeframe=${this.timeframe}&startTime=${startTime}&endTime=${endTime}`;
                    const token = localStorage.getItem('token');
                    const headers = token ? { Authorization: `Bearer ${token}` } : {};
                    const [resp1, resp2] = await Promise.all([
                        axios.get(url1, { headers }),
                        axios.get(url2, { headers })
                    ]);
                    this.kline1 = resp1.data || [];
                    this.kline2 = resp2.data || [];
                    // 对齐时间戳，计算价差
                    const map2 = new Map(this.kline2.map(item => [item.timestamp, item]));
                    this.spread = this.kline1
                        .filter(item => map2.has(item.timestamp))
                        .map(item => ({
                            x: new Date(item.timestamp).getTime(),
                            y: parseFloat(item.closePrice) - parseFloat(map2.get(item.timestamp).closePrice)
                        }));
                    this.renderChart();
                } else if (this.dataType === 'depth') {
                    this.error = '深度数据价差暂未实现';
                }
            } catch (e) {
                this.error = '数据获取失败';
            } finally {
                this.loading = false;
            }
        },
        renderChart() {
            const ctx = document.getElementById('spreadChart');
            ctx.innerHTML = '';
            if (this.chart && this.chart.destroy) this.chart.destroy();
            if (!this.kline1.length || !this.kline2.length) {
                this.error = 'K线数据为空';
                return;
            }
            // K线数据格式转换
            const series1 = this.kline1.map(item => ({
                x: new Date(item.timestamp).getTime(),
                y: [
                    parseFloat(item.openPrice) || 0,
                    parseFloat(item.highPrice) || 0,
                    parseFloat(item.lowPrice) || 0,
                    parseFloat(item.closePrice) || 0
                ]
            }));
            const series2 = this.kline2.map(item => ({
                x: new Date(item.timestamp).getTime(),
                y: [
                    parseFloat(item.openPrice) || 0,
                    parseFloat(item.highPrice) || 0,
                    parseFloat(item.lowPrice) || 0,
                    parseFloat(item.closePrice) || 0
                ]
            }));
            // 价差线
            const spreadLine = this.spread.map(item => ({ x: item.x, y: item.y }));
            this.chart = new ApexCharts(ctx, {
                series: [
                    {
                        name: this.selected1.instId,
                        type: 'candlestick',
                        data: series1
                    },
                    {
                        name: this.selected2.instId,
                        type: 'candlestick',
                        data: series2
                    },
                    {
                        name: '价差',
                        type: 'line',
                        data: spreadLine,
                        yAxisIndex: 1
                    }
                ],
                chart: {
                    height: 500,
                    type: 'line',
                    id: 'spread-mix',
                    toolbar: { show: true }
                },
                stroke: {
                    width: [1, 1, 2],
                    curve: 'straight'
                },
                xaxis: {
                    type: 'datetime'
                },
                yaxis: [
                    {
                        title: { text: '价格' },
                        tooltip: { enabled: true }
                    },
                    {
                        opposite: true,
                        title: { text: '价差' },
                        tooltip: { enabled: true }
                    }
                ],
                tooltip: {
                    shared: true,
                    custom: function({series, seriesIndex, dataPointIndex, w}) {
                        let html = '';
                        if (seriesIndex === 0 || seriesIndex === 1) {
                            html += `<div>收盘价: ${series[seriesIndex][dataPointIndex][3]}</div>`;
                        } else if (seriesIndex === 2) {
                            html += `<div>价差: ${series[2][dataPointIndex]}</div>`;
                        }
                        return html;
                    }
                },
                title: {
                    text: `${this.selected1.instId} vs ${this.selected2.instId} 价差图`,
                    align: 'left'
                }
            });
            this.chart.render();
        }
    }
});
app.mount('#spread-app'); 