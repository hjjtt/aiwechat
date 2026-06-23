/**
 * 位置服务
 * 使用腾讯地图 API 进行逆地理编码
 */

// 腾讯地图 API Key
const QQ_MAP_KEY = "OB4BZ-D4W3U-B7VVO-4PJWW-6TKDJ-WPB77";

// 默认位置（湖南软件职业技大学出版社）
const DEFAULT_LOCATION = {
  latitude: 27.8347,
  longitude: 112.9167,
};

/**
 * 检查位置权限
 * @returns {Promise<boolean>}
 */
function checkLocationPermission() {
  return new Promise((resolve) => {
    wx.getSetting({
      success: (res) => {
        if (res.authSetting["scope.userLocation"]) {
          resolve(true);
        } else {
          // 请求权限
          wx.authorize({
            scope: "scope.userLocation",
            success: () => {
              resolve(true);
            },
            fail: () => {
              // 引导用户去设置页
              wx.showModal({
                title: "需要位置权限",
                content: "请允许小程序使用位置权限，以便为您提供更好的服务",
                confirmText: "去设置",
                success: (modalRes) => {
                  if (modalRes.confirm) {
                    wx.openSetting();
                  }
                },
              });
              resolve(false);
            },
          });
        }
      },
      fail: () => {
        resolve(false);
      },
    });
  });
}

/**
 * 获取当前位置（经纬度）
 * @returns {Promise<{latitude: number, longitude: number, accuracy: number, speed: number, altitude: number}>}
 */
function getCurrentLocation() {
  return new Promise((resolve, reject) => {
    wx.getLocation({
      type: "gcj02", // 坐标系：国测局坐标（火星坐标）
      altitude: true, // 包含高度信息
      accuracy: "high", // 高精度定位
      timeout: 15000, // 超时15秒
      success: (res) => {
        console.log(
          "定位成功:",
          res.latitude,
          res.longitude,
          "精度:",
          res.accuracy,
          "米"
        );
        resolve({
          latitude: res.latitude,
          longitude: res.longitude,
          accuracy: res.accuracy,
          speed: res.speed || 0,
          altitude: res.altitude || 0,
        });
      },
      fail: (err) => {
        console.error("定位失败:", err);
        // 定位失败时使用默认位置
        wx.showToast({
          title: "定位失败，使用默认位置",
          icon: "none",
        });
        resolve({
          latitude: DEFAULT_LOCATION.latitude,
          longitude: DEFAULT_LOCATION.longitude,
          accuracy: 0,
          speed: 0,
          altitude: 0,
          isDefault: true,
        });
      },
    });
  });
}

/**
 * WGS84（GPS原始坐标）转 GCJ02（火星坐标）
 * @param {number} lng - 经度
 * @param {number} lat - 纬度
 */
function wgs84togcj02(lng, lat) {
  const x_PI = (3.14159265358979324 * 3000.0) / 180.0;
  const PI = 3.1415926535897932384626;
  const a = 6378245.0;
  const ee = 0.00669342162296594323;

  let dlat = transformLat(lng - 105.0, lat - 35.0);
  let dlng = transformLng(lng - 105.0, lat - 35.0);
  const radlat = (lat / 180.0) * PI;
  let magic = Math.sin(radlat);
  magic = 1 - ee * magic * magic;
  const sqrtmagic = Math.sqrt(magic);

  dlat = (dlat * 180.0) / ((a * (1 - ee)) / (magic * sqrtmagic) * PI);
  dlng = (dlng * 180.0) / ((a / sqrtmagic) * Math.cos(radlat) * PI);

  return {
    lng: lng + dlng,
    lat: lat + dlat,
  };
}

function transformLat(lng, lat) {
  const x_PI = (3.14159265358979324 * 3000.0) / 180.0;
  const PI = 3.1415926535897932384626;
  const a = 6378245.0;
  const ee = 0.00669342162296594323;

  let ret = -100.0 + 2.0 * lng + 3.0 * lat + 0.2 * lat * lat + 0.1 * lng * lat + 0.2 * Math.sqrt(Math.abs(lng));
  ret += ((20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0) / 3.0;
  ret += ((20.0 * Math.sin(lat * PI) + 40.0 * Math.sin(lat / 3.0 * PI)) * 2.0) / 3.0;
  ret += ((160.0 * Math.sin(lat / 12.0 * PI) + 320 * Math.sin(lat * PI / 30.0)) * 2.0) / 3.0;
  return ret;
}

function transformLng(lng, lat) {
  const x_PI = (3.14159265358979324 * 3000.0) / 180.0;
  const PI = 3.1415926535897932384626;
  const a = 6378245.0;
  const ee = 0.00669342162296594323;

  let ret = 300.0 + lng + 2.0 * lat + 0.1 * lng * lng + 0.1 * lng * lat + 0.1 * Math.sqrt(Math.abs(lng));
  ret += ((20.0 * Math.sin(6.0 * lng * PI) + 20.0 * Math.sin(2.0 * lng * PI)) * 2.0) / 3.0;
  ret += ((20.0 * Math.sin(lng * PI) + 40.0 * Math.sin(lng / 3.0 * PI)) * 2.0) / 3.0;
  ret += ((150.0 * Math.sin(lng / 12.0 * PI) + 300.0 * Math.sin(lng / 30.0 * PI)) * 2.0) / 3.0;
  return ret;
}

/**
 * 逆地理编码 - 调用腾讯地图 API 将坐标转换为地址文字
 * @param {number} latitude - 纬度
 * @param {number} longitude - 经度
 * @returns {Promise<Object>}
 */
function reverseGeocoding(latitude, longitude) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: `https://apis.map.qq.com/ws/geocoder/v1/`,
      method: "GET",
      data: {
        location: `${latitude},${longitude}`,
        key: QQ_MAP_KEY,
        get_poi: 0,
      },
      success: (res) => {
        if (res.data && res.data.status === 0) {
          const result = res.data.result;
          resolve({
            formattedAddress: result.address,
            province: result.ad_info.province,
            city: result.ad_info.city,
            district: result.ad_info.district,
            street: result.address_component.street,
            streetNumber: result.address_component.street_number,
            detailAddress: result.address_component.street + (result.address_component.street_number || ""),
            latitude: latitude,
            longitude: longitude,
          });
        } else {
          console.error("逆地理编码失败:", res.data);
          // 失败时使用简单解析
          resolve({
            formattedAddress: `${latitude.toFixed(4)}, ${longitude.toFixed(4)}`,
            province: "",
            city: "",
            district: "",
            street: "",
            streetNumber: "",
            detailAddress: "",
            latitude: latitude,
            longitude: longitude,
          });
        }
      },
      fail: (err) => {
        console.error("逆地理编码请求失败:", err);
        resolve({
          formattedAddress: `${latitude.toFixed(4)}, ${longitude.toFixed(4)}`,
          province: "",
          city: "",
          district: "",
          street: "",
          streetNumber: "",
          detailAddress: "",
          latitude: latitude,
          longitude: longitude,
        });
      },
    });
  });
}

/**
 * 解析地址字符串，提取省市区
 * @param {string} address - 完整地址字符串
 */
function parseAddress(address) {
  if (!address) {
    return { province: "", city: "", district: "", street: "" };
  }

  const provinceSuffixes = ["省", "自治区"];
  const citySuffixes = ["市", "自治州", "地区", "盟"];
  const districtSuffixes = ["区", "县", "旗", "市"];

  let province = "";
  let city = "";
  let district = "";
  let street = address;

  // 提取省份
  for (const suffix of provinceSuffixes) {
    const idx = address.indexOf(suffix);
    if (idx > 0) {
      province = address.substring(0, idx + 1);
      street = address.substring(idx + 1);
      break;
    }
    if (suffix === "自治区") {
      const idx2 = address.indexOf(suffix);
      if (idx2 > 0) {
        province = address.substring(0, idx2 + 3);
        street = address.substring(idx2 + 3);
        break;
      }
    }
  }

  // 提取城市
  for (const suffix of citySuffixes) {
    const idx = street.indexOf(suffix);
    if (idx > 0) {
      city = street.substring(0, idx + 1);
      district = street.substring(idx + 1);
      break;
    }
  }

  // 提取区县
  for (const suffix of districtSuffixes) {
    const idx = district.indexOf(suffix);
    if (idx > 0) {
      district = district.substring(0, idx + 1);
      break;
    }
  }

  return {
    province: province || "",
    city: city || "",
    district: district || "",
    street: street || "",
  };
}

/**
 * 地理编码（地址转经纬度）
 * @param {string} address - 地址
 */
async function geocode(address) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: `https://apis.map.qq.com/ws/geocoder/v1/`,
      method: "GET",
      data: {
        address: address,
        key: QQ_MAP_KEY,
      },
      success: (res) => {
        if (res.data && res.data.status === 0) {
          const location = res.data.result.location;
          resolve({
            latitude: location.lat,
            longitude: location.lng,
          });
        } else {
          reject(new Error(res.data?.message || "地理编码失败"));
        }
      },
      fail: (err) => {
        reject(err);
      },
    });
  });
}

/**
 * 搜索地点 - 使用微信地图选择器
 * @param {string} keyword - 关键词
 */
async function searchPlace(keyword) {
  return new Promise((resolve, reject) => {
    wx.chooseLocation({
      keyword: keyword,
      success: (res) => {
        resolve([
          {
            name: res.name,
            address: res.address,
            latitude: res.latitude,
            longitude: res.longitude,
          },
        ]);
      },
      fail: () => {
        resolve([]);
      },
    });
  });
}

/**
 * 选择位置（调用微信原生位置选择器）
 * @returns {Promise<{name: string, address: string, latitude: number, longitude: number}>}
 */
function chooseLocation() {
  return new Promise((resolve, reject) => {
    wx.chooseLocation({
      success: (res) => {
        resolve({
          name: res.name,
          address: res.address,
          latitude: res.latitude,
          longitude: res.longitude,
        });
      },
      fail: (err) => {
        if (err.errMsg && err.errMsg.includes("cancel")) {
          resolve(null);
        } else {
          console.error("选择位置失败:", err);
          reject(err);
        }
      },
    });
  });
}

/**
 * 打开地图导航
 * @param {number} latitude - 目标纬度
 * @param {number} longitude - 目标经度
 * @param {string} name - 目标名称
 * @param {string} address - 目标地址
 */
function openNavigation(latitude, longitude, name, address) {
  wx.openLocation({
    latitude,
    longitude,
    name,
    address,
    scale: 18,
  });
}

/**
 * 获取当前位置并解析为地址
 * @returns {Promise<Object>} 包含地址信息的对象
 */
async function getCurrentAddress() {
  try {
    // 先检查权限
    const hasPermission = await checkLocationPermission();
    if (!hasPermission) {
      throw new Error("未获得位置权限");
    }

    const location = await getCurrentLocation();
    const addressInfo = await reverseGeocoding(
      location.latitude,
      location.longitude
    );
    return {
      latitude: location.latitude,
      longitude: location.longitude,
      accuracy: location.accuracy,
      speed: location.speed,
      altitude: location.altitude,
      isDefault: location.isDefault || false,
      ...addressInfo,
    };
  } catch (error) {
    console.error("获取地址失败:", error);
    throw error;
  }
}

/**
 * 计算两点间的距离（米）
 * @param {number} lat1 - 起点纬度
 * @param {number} lng1 - 起点经度
 * @param {number} lat2 - 终点纬度
 * @param {number} lng2 - 终点经度
 */
function calculateDistance(lat1, lng1, lat2, lng2) {
  const R = 6371000;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLng = ((lng2 - lng1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLng / 2) *
      Math.sin(dLng / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return Math.round(R * c);
}

module.exports = {
  checkLocationPermission,
  getCurrentLocation,
  reverseGeocoding,
  reverseGeocode: reverseGeocoding, // 兼容旧版本
  wgs84togcj02,
  geocode,
  searchPlace,
  chooseLocation,
  openNavigation,
  getCurrentAddress,
  calculateDistance,
  parseAddress,
  DEFAULT_LOCATION,
};
