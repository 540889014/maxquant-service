Vue.component('line-chart', {
    extends: VueChartJs.Line,
    props: ['chartdata', 'options'],
    mounted () {
        this.renderChart(this.chartdata, this.options)
    }
});

new Vue({
    el: '#backtest-report-app',
    data: {
        strategyName: '',
        timestamp: '',
        performance: {},
        orders: [],
        trades: [],
        periodicPerformance: [],
        dailyIndicators: [],
        portfolioDetails: [],
        chartData: null,
        chartOptions: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                xAxes: [{
                    type: 'time',
                    time: {
                        tooltipFormat: 'll HH:mm'
                    },
                    scaleLabel: {
                        display: true,
                        labelString: 'Date'
                    }
                }],
                yAxes: [{
                    scaleLabel: {
                        display: true,
                        labelString: 'Portfolio Value'
                    }
                }]
            }
        }
    },
    mounted() {
        const urlParams = new URLSearchParams(window.location.search);
        this.strategyName = urlParams.get('strategyName');
        this.timestamp = urlParams.get('timestamp');

        if (this.strategyName && this.timestamp) {
            this.fetchReportData();
        }
    },
    methods: {
        fetchReportData() {
            axios.get(`/api/backtest-reports/${this.strategyName}/${this.timestamp}`)
                .then(response => {
                    const data = response.data;
                    this.performance = data.performance;
                    this.orders = data.orders;
                    this.trades = data.trades;
                    this.periodicPerformance = data.periodicPerformance;
                    this.dailyIndicators = data.dailyIndicators;
                    this.portfolioDetails = data.portfolioDetails;

                    this.prepareChartData();
                })
                .catch(error => {
                    console.error("Error fetching backtest report data:", error);
                    alert('Failed to load backtest report.');
                });
        },
        prepareChartData() {
            if (this.portfolioDetails && this.portfolioDetails.length > 0) {
                // Assuming 'datetime' and 'equity_curve' columns exist.
                // This may need adjustment based on the actual CSV columns.
                const labels = this.portfolioDetails.map(d => d.datetime || d.trade_date);
                const data = this.portfolioDetails.map(d => parseFloat(d.equity_curve || d.portfolio_value));

                this.chartData = {
                    labels: labels,
                    datasets: [{
                        label: 'Portfolio Value',
                        backgroundColor: 'rgba(54, 162, 235, 0.5)',
                        borderColor: 'rgb(54, 162, 235)',
                        data: data,
                        fill: false,
                    }]
                };
            }
        }
    }
}); 