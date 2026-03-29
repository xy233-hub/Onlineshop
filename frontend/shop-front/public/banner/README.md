# 轮播图图片目录

## 说明

此目录用于存放商城首页轮播图的图片。

## 使用方法

1. 在本目录中添加图片文件，建议使用以下命名格式：
   - image1.jpg
   - image2.jpg
   - image3.jpg
   - 以此类推

2. 图片尺寸建议：
   - 宽度：至少1200px
   - 高度：400px左右
   - 格式：JPG、PNG或WebP

3. 添加新图片后，需要修改 `src/views/Home.vue` 文件中的 `fetchBannerProducts` 函数，更新轮播图配置。

4. 配置示例：
   ```javascript
   bannerItems.value = [
     {
       id: 1,
       image: '/banner/image1.jpg',
       title: '限时优惠',
       subtitle: '全场商品低至5折',
       buttonText: '立即抢购'
     },
     // 更多图片配置...
   ]
   ```

## 注意事项

- 图片文件大小建议控制在200KB以内，以确保页面加载速度
- 确保图片内容与商城主题相关
- 定期更新轮播图内容，保持页面新鲜感