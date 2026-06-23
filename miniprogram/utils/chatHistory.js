/**
 * 聊天历史记录本地存储工具
 * 用于在微信小程序本地存储中保存用户的聊天历史
 */

const CHAT_HISTORY_KEY = 'chat_history_';
const MAX_HISTORY_COUNT = 50; // 最多保存50条对话记录
const MAX_MESSAGES_PER_HISTORY = 20; // 每条历史记录最多保存20条消息

/**
 * 保存聊天历史
 * @param {string} userId - 用户ID
 * @param {Array} messages - 消息数组
 */
export function saveChatHistory(userId, messages) {
  if (!userId || !messages || messages.length === 0) {
    return;
  }

  const key = CHAT_HISTORY_KEY + userId;
  const historyList = getChatHistoryList(userId);

  // 创建新的历史记录
  const newHistory = {
    id: Date.now(),
    timestamp: new Date().toISOString(),
    messages: messages.slice(-MAX_MESSAGES_PER_HISTORY), // 保留最近的消息
    preview: messages.length > 0 ? messages[messages.length - 1].content : '', // 最后一条消息作为预览
  };

  // 添加到列表开头
  historyList.unshift(newHistory);

  // 限制历史记录数量
  const trimmedHistory = historyList.slice(0, MAX_HISTORY_COUNT);

  try {
    wx.setStorageSync(key, trimmedHistory);
  } catch (e) {
    console.error('保存聊天历史失败:', e);
  }
}

/**
 * 获取聊天历史列表
 * @param {string} userId - 用户ID
 * @returns {Array} 历史记录列表
 */
export function getChatHistoryList(userId) {
  if (!userId) {
    return [];
  }

  const key = CHAT_HISTORY_KEY + userId;

  try {
    const data = wx.getStorageSync(key);
    return data || [];
  } catch (e) {
    console.error('获取聊天历史列表失败:', e);
    return [];
  }
}

/**
 * 获取指定历史记录的详细信息
 * @param {string} userId - 用户ID
 * @param {number} historyId - 历史记录ID
 * @returns {Object|null} 历史记录详情
 */
export function getChatHistoryDetail(userId, historyId) {
  const historyList = getChatHistoryList(userId);
  return historyList.find(item => item.id === historyId) || null;
}

/**
 * 删除指定历史记录
 * @param {string} userId - 用户ID
 * @param {number} historyId - 历史记录ID
 */
export function deleteChatHistory(userId, historyId) {
  const key = CHAT_HISTORY_KEY + userId;
  const historyList = getChatHistoryList(userId);

  const filteredList = historyList.filter(item => item.id !== historyId);

  try {
    wx.setStorageSync(key, filteredList);
  } catch (e) {
    console.error('删除聊天历史失败:', e);
  }
}

/**
 * 清除用户所有聊天历史
 * @param {string} userId - 用户 ID
 */
export function clearChatHistory(userId) {
  if (!userId) {
    return;
  }

  const key = CHAT_HISTORY_KEY + userId;

  try {
    wx.removeStorageSync(key);
  } catch (e) {
    console.error('清除聊天历史失败:', e);
  }
}

/**
 * 清空所有聊天历史（别名）
 * @param {string} userId - 用户 ID
 */
export function clearAllChatHistory(userId) {
  return clearChatHistory(userId);
}

/**
 * 更新指定历史记录的消息
 * @param {string} userId - 用户ID
 * @param {number} historyId - 历史记录ID
 * @param {Array} messages - 新的消息数组
 */
export function updateChatHistoryMessages(userId, historyId, messages) {
  const key = CHAT_HISTORY_KEY + userId;
  const historyList = getChatHistoryList(userId);

  const index = historyList.findIndex(item => item.id === historyId);
  if (index !== -1) {
    historyList[index].messages = messages.slice(-MAX_MESSAGES_PER_HISTORY);
    historyList[index].preview = messages.length > 0 ? messages[messages.length - 1].content : '';
    historyList[index].timestamp = new Date().toISOString();

    try {
      wx.setStorageSync(key, historyList);
    } catch (e) {
      console.error('更新聊天历史失败:', e);
    }
  }
}

export default {
  saveChatHistory,
  getChatHistoryList,
  getChatHistoryDetail,
  deleteChatHistory,
  clearChatHistory,
  clearAllChatHistory,
  updateChatHistoryMessages,
};
