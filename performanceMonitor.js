// 性能监控工具
class PerformanceMonitor {
  constructor() {
    this.metrics = {
      pageLoadTime: 0,
      apiRequests: [],
      componentRenderTimes: [],
      memoryUsage: 0,
      // 新增核心 Web 指标
      fcp: 0, // 首次内容绘制
      lcp: 0, // 最大内容绘制
      fid: 0, // 首次输入延迟
      cls: 0, // 累积布局偏移
      inp: 0, // 交互到下一个绘制
      // 资源加载统计
      resourceStats: {
        images: 0,
        scripts: 0,
        stylesheets: 0,
        fonts: 0,
        totalSize: 0
      }
    };
    this.init();
  }

  init() {
    // 监控页面加载性能
    this.monitorPageLoad();
    
    // 监控内存使用
    this.monitorMemory();
    
    // 监控核心 Web 指标
    this.monitorCoreWebVitals();
    
    // 监控资源加载
    this.monitorResourceLoad();
  }

  // 监控页面加载性能
  monitorPageLoad() {
    if (window.performance) {
      window.addEventListener('load', () => {
        const performanceData = window.performance.timing;
        this.metrics.pageLoadTime = performanceData.loadEventEnd - performanceData.navigationStart;
        console.log('[性能监控] 页面加载时间:', this.metrics.pageLoadTime + 'ms');
      });
      
      window.addEventListener('DOMContentLoaded', () => {
        const performanceData = window.performance.timing;
        const domContentLoadedTime = performanceData.domContentLoadedEventEnd - performanceData.navigationStart;
        console.log('[性能监控] DOM 内容加载完成时间:', domContentLoadedTime + 'ms');
      });
    }
  }

  // 监控核心 Web 指标
  monitorCoreWebVitals() {
    // FCP 和 LCP 监控
    if ('PerformanceObserver' in window) {
      // FCP 监控
      new PerformanceObserver((entryList) => {
        const entries = entryList.getEntriesByName('first-contentful-paint');
        if (entries.length) {
          this.metrics.fcp = entries[0].startTime;
          console.log('[性能监控] 首次内容绘制 (FCP):', this.metrics.fcp + 'ms');
        }
      }).observe({ entryTypes: ['paint'] });

      // LCP 监控
      new PerformanceObserver((entryList) => {
        const entries = entryList.getEntriesByName('largest-contentful-paint');
        if (entries.length) {
          this.metrics.lcp = entries[0].startTime;
          console.log('[性能监控] 最大内容绘制 (LCP):', this.metrics.lcp + 'ms');
        }
      }).observe({ entryTypes: ['largest-contentful-paint'] });

      // CLS 监控
      let clsValue = 0;
      new PerformanceObserver((entryList) => {
        entryList.getEntries().forEach((entry) => {
          if (!entry.hadRecentInput) {
            clsValue += entry.value;
            this.metrics.cls = clsValue;
            console.log('[性能监控] 累积布局偏移 (CLS):', this.metrics.cls);
          }
        });
      }).observe({ entryTypes: ['layout-shift'] });

      // FID 监控
      new PerformanceObserver((entryList) => {
        const entries = entryList.getEntries();
        if (entries.length) {
          this.metrics.fid = entries[0].processingStart - entries[0].startTime;
          console.log('[性能监控] 首次输入延迟 (FID):', this.metrics.fid + 'ms');
        }
      }).observe({ entryTypes: ['first-input'] });
      
      // INP 监控 (交互到下一个绘制)
      const interactionEntries = [];
      new PerformanceObserver((entryList) => {
        interactionEntries.push(...entryList.getEntries());
        const longest = interactionEntries.reduce((a, b) => 
          a.duration > b.duration ? a : b, interactionEntries[0]);
        if (longest) {
          this.metrics.inp = longest.duration;
          console.log('[性能监控] 交互到下一个绘制 (INP):', this.metrics.inp + 'ms');
        }
      }).observe({ entryTypes: ['event'] });
    }
  }

  // 监控资源加载
  monitorResourceLoad() {
    if ('PerformanceObserver' in window) {
      new PerformanceObserver((entryList) => {
        const entries = entryList.getEntries();
        entries.forEach((entry) => {
          const type = entry.initiatorType;
          const size = entry.transferSize || 0;
          
          // 更新资源统计
          if (type === 'img') this.metrics.resourceStats.images++;
          else if (type === 'script') this.metrics.resourceStats.scripts++;
          else if (type === 'link' && entry.name.endsWith('.css')) this.metrics.resourceStats.stylesheets++;
          else if (type === 'font') this.metrics.resourceStats.fonts++;
          
          this.metrics.resourceStats.totalSize += size;
        });
      }).observe({ entryTypes: ['resource'] });
    }
  }

  // 监控内存使用
  monitorMemory() {
    if (performance.memory) {
      setInterval(() => {
        this.metrics.memoryUsage = Math.round(performance.memory.usedJSHeapSize / 1024 / 1024 * 100) / 100;
        console.log('[性能监控] 内存使用:', this.metrics.memoryUsage + 'MB');
      }, 30000); // 每30秒检查一次
    }
  }

  // 监控API请求
  monitorApiRequest(url, method, duration, status) {
    const requestData = {
      url,
      method,
      duration,
      status,
      timestamp: new Date().toISOString()
    };
    
    this.metrics.apiRequests.push(requestData);
    
    // 只保留最近100个请求
    if (this.metrics.apiRequests.length > 100) {
      this.metrics.apiRequests.shift();
    }
    
    console.log(`[性能监控] API请求: ${method} ${url} - ${status} (${duration}ms)`);
  }

  // 监控组件渲染时间
  monitorComponentRender(componentName, duration) {
    const renderData = {
      componentName,
      duration,
      timestamp: new Date().toISOString()
    };
    
    this.metrics.componentRenderTimes.push(renderData);
    
    // 只保留最近50个渲染记录
    if (this.metrics.componentRenderTimes.length > 50) {
      this.metrics.componentRenderTimes.shift();
    }
    
    console.log(`[性能监控] 组件渲染: ${componentName} - ${duration}ms`);
  }

  // 获取所有性能指标
  getMetrics() {
    return this.metrics;
  }

  // 清空性能指标
  clearMetrics() {
    this.metrics = {
      pageLoadTime: 0,
      apiRequests: [],
      componentRenderTimes: [],
      memoryUsage: 0
    };
  }

  // 导出性能报告
  exportReport() {
    const report = {
      timestamp: new Date().toISOString(),
      metrics: this.metrics,
      userAgent: navigator.userAgent
    };
    
    return JSON.stringify(report, null, 2);
  }
}

// 创建单例实例
const performanceMonitor = new PerformanceMonitor();

export default performanceMonitor;