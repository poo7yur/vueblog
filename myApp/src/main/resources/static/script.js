// 全局变量，用于存储从接口获取的根数据
let rootData = null;
let currentQuery = {
  keyword: "",
  pageNum: 1,
  pageSize: 10,
};

// 新增：当前评论图片信息
let currentCommentImg = {
  src: "",
  fileName: "",
  imgId: ""
};

// 页面加载完成后执行
document.addEventListener("DOMContentLoaded", function () {
  fetchData();
  initUserMenu();
});

/*拦截每个请求 请求头放上token*/
function getToken() {
  // 按你项目实际存储位置来：
  // localStorage / sessionStorage / cookie / indexedDB …
  return localStorage.getItem("userToken") || "";
}

const originalFetch = window.fetch;
window.fetch = function (url, opts = {}) {
  // 保证 headers 存在且是 Headers 实例
  const headers = new Headers(opts.headers || {});
  // 塞 token（没有就空串）
  headers.set("token", getToken());

  return originalFetch(url, {
    ...opts,
    headers,
  });
};

/* 拦截原生 XMLHttpRequest*/
const originalOpen = XMLHttpRequest.prototype.open;
const originalSetRequestHeader = XMLHttpRequest.prototype.setRequestHeader;

XMLHttpRequest.prototype.open = function (method, url, async, user, password) {
  // 先保存真正的 open 参数，后面要用
  this._url = url;
  this._method = method;
  // 给当前实例打一个标记，表示我们还没塞 token
  this._tokenAdded = false;
  return originalOpen.apply(this, arguments);
};

XMLHttpRequest.prototype.setRequestHeader = function (header, value) {
  // 如果业务自己写了 token，就尊重它
  if (header.toLowerCase() === "token") {
    this._tokenAdded = true;
  }
  return originalSetRequestHeader.call(this, header, value);
};

// 在 send 时统一补 token（保证是最后一步）
const originalSend = XMLHttpRequest.prototype.send;
XMLHttpRequest.prototype.send = function (body) {
  if (!this._tokenAdded) {
    originalSetRequestHeader.call(this, "token", getToken());
  }
  return originalSend.call(this, body);
};

// 1. 获取目录树数据
function fetchData() {
  fetch("/scanner")
    .then((response) => response.json())
    .then((data) => {
      if (data.code === 0) {
        rootData = data.data;
        renderTree(rootData, document.getElementById("treeContainer"));
      }
    })
    .catch((err) => {
      document.getElementById(
        "contentArea"
      ).innerHTML = `<p style="color:red">接口请求失败: ${err}</p>`;
    });
}

// 2. 递归渲染左侧树状菜单
function renderTree(node, container) {
  const ul = document.createElement("ul");
  ul.className = "tree";

  node.children.forEach((child) => {
    const li = document.createElement("li");

    if (child.children && child.children.length > 0) {
      // 父节点 (有子文件夹)
      const span = document.createElement("span");
      span.className = "toggle";
      span.textContent = `${child.name} ▼`;
      li.appendChild(span);

      const subMenu = document.createElement("div");
      renderTree(child, subMenu);
      subMenu.style.display = "none";
      li.appendChild(subMenu);

      span.addEventListener("click", function (e) {
        e.stopPropagation();
        const isBlock = subMenu.style.display === "block";
        subMenu.style.display = isBlock ? "none" : "block";
        span.textContent = `${child.name} ${isBlock ? "▼" : "▲"}`;
      });
    } else {
      // 叶子节点 (末级文件夹)，绑定点击事件
      li.textContent = child.name;
      li.onclick = function () {
        loadFolder(child.path); // 传入文件夹全路径
      };
    }

    ul.appendChild(li);
  });

  container.appendChild(ul);
}

// 3. 加载并显示图片
function loadFolder(folderPath) {
  const contentArea = document.getElementById("contentArea");
  contentArea.innerHTML = "<p>加载中...</p>";
  const postData = {
    path: folderPath,
  };

  fetch("/listImages", {
    method: "POST",
    headers: {
      "Content-Type": "application/json; charset=utf-8",
    },
    body: JSON.stringify(postData),
  })
    .then((response) => response.json())
    .then((imgData) => {
      const urls = imgData.data.images;
      if (imgData.code === 0 && Array.isArray(urls)) {
        renderImageGrid(urls);
      } else {
        contentArea.innerHTML = "<p>该文件夹为空或发生错误。</p>";
      }
    })
    .catch((err) => {
      contentArea.innerHTML = `<p style="color:red">图片加载失败: ${err}</p>`;
    });
}

// 4. 渲染图片平铺列表
// filePathList: 接口返回的全路径数组
function renderImageGrid(filePathList) {
  const contentArea = document.getElementById("contentArea");

  // 1. 转换路径：将磁盘全路径转换为 Web 访问路径
  // 原始路径: C:\Users\Admin\Pictures\save\A\A02\image_2.jpg
  // 目标路径: /static/A/A02/image_2.jpg
  const baseUrl = "/static/";
  const webPathList = filePathList.map((fullPath) => {
    // 将 Windows 路径分隔符 \ 替换为 / (防止路径显示错误)
    let webPath = fullPath.replace(/\\/g, "/");

    // 关键步骤：截取 "save" 目录之后的部分，并拼接到 baseUrl 后面
    // 假设后端映射的是 save 目录，所以我们需要去掉 "C:/Users/Admin/Pictures/save"
    const saveIndex = webPath.indexOf("/save/");

    if (saveIndex !== -1) {
      // 从 '/save/' 之后开始截取，并拼接到 /static/ 后面
      return baseUrl + webPath.substring(saveIndex + 6); // +6 是为了去掉 '/save/' 这6个字符
    } else {
      // 如果找不到 save，直接返回原路径（或者做其他容错处理）
      return webPath;
    }
  });

  // 使用 CSS Grid 进行平铺布局
  let html = `<div class="image-grid">`;
  webPathList.forEach((src) => {
    // 获取文件名作为 path 参数发送给后端
    const fileName = src.substring(src.lastIndexOf("/") + 1);
    const hasUnderscore = src.includes("share");
    const token = localStorage.getItem("userToken");
    const heartHtml = token && hasUnderscore
      ? `<div class="like-animation" id="like-${fileName}" onclick="handleLike(event, '${src}', '${fileName}')">❤️</div>`
      : "";
    html += `
          <div class="image-item">
                         <img src="${src}" onclick="openImage('${src}')">
                         ${heartHtml}
           </div>
      `;
  });
  html += `</div>`;

  contentArea.innerHTML = html;
}

//点赞逻辑
async function handleLike(event, imgSrc, fileName) {
  event.stopPropagation();

  const heartEl = document.getElementById(`like-${fileName}`);
  if (!heartEl) return;

  // 触发动画
  heartEl.style.display = "block";
  heartEl.style.animation = "none";
  void heartEl.offsetWidth; // 强制重排
  heartEl.style.animation = "likePop 0.8s ease-out forwards";

  // 调用后端接口
  try {
    const response = await fetch("/likeImage", {
      method: "POST",
      headers: {
        "Content-Type": "application/json; charset=utf-8",
        token: localStorage.getItem("token"),
      },
      body: JSON.stringify({ path: fileName }),
    });

    const result = await response.json();
    alert(result.data);

    setTimeout(() => {
      heartEl.style.display = "none"; // 动画结束后隐藏
    }, 2000);
  } catch (error) {
    alert(error);
    heartEl.style.display = "none"; // 即使失败也隐藏动画
    setTimeout(() => {
      alert("请检查网络或权限");
    }, 50);
  }
}

function openImage(src) {
  // 创建遮罩层（重命名为image-modal）
  const modal = document.createElement("div");
  modal.className = "image-modal";

  // 判断是否为share目录图片（决定是否显示评论区）
  const isShareImg = src.includes("share");
  const token = localStorage.getItem("userToken");
  const fileName = src.substring(src.lastIndexOf("/") + 1);
  const imgId = fileName.split("_")[0];
  currentCommentImg = {
    src: src,
    fileName: fileName,
    imgId: imgId
  };

  // 构建大图+评论区DOM
  modal.innerHTML = `
    <div class="image-modal-left">
      <img src="${src}" class="image-modal-img">
    </div>
    <div class="image-modal-right" id="commentPanel">
      <div class="comment-panel-header">评论区</div>
      <div class="comment-panel-list" id="commentList"></div>
      <div class="comment-panel-input-area">
        <input type="text" class="comment-panel-input" id="commentInput" placeholder="输入你的评论...">
        <button class="comment-panel-submit" id="commentSubmit">提交</button>
      </div>
    </div>
  `;

  // 点击遮罩层关闭
  modal.onclick = (e) => {
    if (e.target === modal) {
      document.body.removeChild(modal);
    }
  };

  document.body.appendChild(modal);

  // 仅share目录+已登录显示评论区并加载评论
  if (isShareImg && token) {
    const commentPanel = document.getElementById("commentPanel");
    commentPanel.style.display = "flex";
    // 加载评论列表
    loadComments(imgId);
    // 绑定提交评论事件
    document.getElementById("commentSubmit").addEventListener("click", submitComment);
  }
}

// 加载评论列表
async function loadComments(imgId) {
  const commentListEl = document.getElementById("commentList");
  commentListEl.innerHTML = '<div class="comment-loading">加载中...</div>';

  try {
    const response = await fetch(`/getComment/${imgId}`);
    const result = await response.json();

    if (result.code !== 0) {
      commentListEl.innerHTML = `<div class="comment-empty">加载失败：${result.msg}</div>`;
      return;
    }

    const comments = result.data;
    if (comments.length === 0) {
      commentListEl.innerHTML = '<div class="comment-empty">暂无评论，快来抢沙发～</div>';
      return;
    }

    // 渲染评论列表
    let commentHtml = "";
    comments.forEach(comment => {
      commentHtml += `
        <div class="comment-panel-item">
          <div class="comment-panel-author">${comment.commentBy}</div>
          <div class="comment-panel-content">${comment.content}</div>
        </div>
      `;
    });
    commentListEl.innerHTML = commentHtml;
  } catch (error) {
    console.error("加载评论失败：", error);
    commentListEl.innerHTML = '<div class="comment-empty">加载评论失败，请重试</div>';
  }
}

// 提交评论
async function submitComment() {
  const commentInput = document.getElementById("commentInput");
  const remark = commentInput.value.trim();

  // 校验输入
  if (!remark) {
    alert("请输入评论内容！");
    return;
  }

  // 校验登录状态（兜底）
  const token = localStorage.getItem("userToken");
  if (!token) {
    alert("请先登录后再评论！");
    document.querySelector(".image-modal").remove();
    document.getElementById("loginModalMask").style.display = "flex";
    return;
  }

  try {
    const response = await fetch("/commentImage", {
      method: "POST",
      headers: {
        "Content-Type": "application/json; charset=utf-8"
        // token已通过全局fetch拦截器自动添加，无需手动传
      },
      body: JSON.stringify({
        path: currentCommentImg.fileName,
        remark: remark
      })
    });

    const result = await response.json();
    if (result.code === 0) {
      alert("评论成功！");
      commentInput.value = ""; // 清空输入框
      await loadComments(currentCommentImg.imgId); // 刷新评论列表
    } else {
      alert(`评论失败：${result.msg}`);
    }
  } catch (error) {
    console.error("提交评论失败：", error);
    alert("提交评论失败，请检查网络或接口是否正常！");
  }
}

// 获取DOM元素
const userIcon = document.getElementById("userIcon");
const userMenu = document.getElementById("userMenu");

// 给用户图标添加点击事件（切换菜单显示/隐藏）
userIcon.addEventListener("click", function (e) {
  // 阻止事件冒泡（避免触发document的点击事件，导致菜单刚显示就隐藏）
  e.stopPropagation();
  // 切换active类，控制菜单显示/隐藏
  userMenu.classList.toggle("active");
});

// 给document添加点击事件（点击其他地方隐藏菜单）
document.addEventListener("click", function () {
  // 移除active类，隐藏菜单
  userMenu.classList.remove("active");
});

//  给菜单本身添加点击事件（阻止事件冒泡，避免点击菜单项时菜单隐藏）
userMenu.addEventListener("click", function (e) {
  e.stopPropagation();
});

//登录事件
const loginLink = document.getElementById("loginLink");
const loginModalMask = document.getElementById("loginModalMask");
const closeModal = document.getElementById("closeModal");
const loginBtn = document.getElementById("loginBtn");
const usernameInput = document.getElementById("username");
const passwordInput = document.getElementById("password");

//打开登录弹窗（点击登录链接）
loginLink.addEventListener("click", (e) => {
  // 阻止锚点跳转（#login）
  e.preventDefault();
  // 隐藏用户菜单，显示登录弹窗
  //  userMenu.style.display = "none";
  loginModalMask.style.display = "flex";
});

// 关闭登录弹窗（点击取消/遮罩）
closeModal.addEventListener("click", () => {
  loginModalMask.style.display = "none";
});

// 核心：提交登录请求，获取并存储Token
loginBtn.addEventListener("click", async () => {
  // （1）获取表单数据
  const username = usernameInput.value.trim();
  const password = passwordInput.value.trim();

  // 表单验证
  if (!username || !password) {
    alert("用户名和密码不能为空！");
    return;
  }

  try {
    // （2）发送POST登录请求（与后端接口一致）
    const response = await fetch("/login", {
      method: "POST",
      headers: {
        "Content-Type": "application/json; charset=utf-8",
      },
      body: JSON.stringify({
        // 构造请求体，与后端参数对应
        username: username,
        password: password,
      }),
    });

    // （3）处理响应结果
    if (!response.ok) {
      throw new Error(`请求失败：${response.status} ${response.statusText}`);
    }

    const result = await response.json(); // 解析后端返回的JSON数据
    if (result.code !== 0) {
      alert(result.msg);
      return;
    }
    const token = result.data.token; // 提取Token（假设后端返回格式：{ "token": "xxx.yyy.zzz" }）
    const userId = result.data.userId;

    // （4）存储Token（两种常用方式，按需选择）
    // 方式1：localStorage - 持久化存储（关闭浏览器后仍存在，需手动清除）
    localStorage.setItem("userToken", token);
    localStorage.setItem("currentUser", username);
    localStorage.setItem("currentUserId", userId);

    // 启动token刷新监控
    startTokenRefreshMonitor();
    //alert("登录成功！Token已存储");
    loginModalMask.style.display = "none"; // 关闭弹窗
    // 关键：登录成功后调用视图更新函数
    updateUserView(username, userId);
    window.location.reload();
  } catch (error) {
    console.error("登录异常：", error);
    alert("登录失败：" + error.message);
  }
});

/*更新用户视图*/
function updateUserView(username, userId) {
  // 4.1 修改userIcon的title为当前用户名（鼠标悬浮显示）
  userIcon.setAttribute("title", username);

  // 4.2 隐藏「注册」选项（两种方式可选，推荐方式1）
  // 方式1：直接隐藏<li>标签，彻底不显示
  registerLink.parentElement.style.display = "none";
  // 方式2：移除整个注册<li>标签（不可逆，如需恢复需重新创建）
  // registerLink.parentElement.remove();

  // 4.3 隐藏「登录」选项（登录后无需再显示登录）
  loginLink.parentElement.style.display = "none";

  // 4.4 存储 userId（后续调用消息接口需要）
  if (userId) {
    localStorage.setItem("currentUserId", userId);
  }
}

/*页面初始化：判断是否已登录，恢复视图状态（避免刷新页面后还原）*/
window.onload = function () {
  const currentUser = localStorage.getItem("currentUser");
  const userToken = localStorage.getItem("userToken");

  // 若存在有效Token和用户名，自动更新视图
  if (currentUser && userToken) {
    updateUserView(currentUser, localStorage.getItem("currentUserId"));
  }
};

// 监听注册链接点击事件
document.getElementById("registerLink").addEventListener("click", function (e) {
  e.preventDefault();
  document.getElementById("registerModalMask").style.display = "flex";
});

// 监听注册按钮点击事件
document.getElementById("registerBtn").addEventListener("click", function () {
  const username = document.getElementById("regUsername").value;
  const password = document.getElementById("regPassword").value;
  const email = document.getElementById("regEmail").value;
  const phone = document.getElementById("regPhone").value;

  // 发送 POST 请求
  fetch("/addUser", {
    method: "POST",
    headers: {
      "Content-Type": "application/json; charset=utf-8",
    },
    body: JSON.stringify({
      name: username,
      email: email,
      password: password,
      phone: phone,
    }),
  })
    .then((response) => response.json())
    .then((data) => {
      alert("注册成功！,请登陆后使用");
      document.getElementById("registerModalMask").style.display = "none";
    })
    .catch((error) => {
      console.error("Error:", error);
      alert("注册失败，请重试！");
    });
});

/*消息弹窗交互*/
msgLink.addEventListener("click", async (e) => {
  e.preventDefault(); // 阻止锚点跳转
  userMenu.style.display = "none"; // 先隐藏下拉菜单

  // 第一步：验证登录状态（是否有 Token 和 userId）
  const userToken = localStorage.getItem("userToken");
  const currentUserId = localStorage.getItem("currentUserId");
  if (!userToken || !currentUserId) {
    alert("请先登录后再查看消息！");
    loginModalMask.style.display = "flex"; // 直接弹出登录框
    return;
  }

  // 第二步：打开消息弹窗并加载数据
  msgModal.style.display = "flex";
  await fetchMessages();
});

// 6.2 点击关闭按钮/弹窗遮罩关闭弹窗
closeMsgModal.addEventListener("click", () => {
  msgModal.style.display = "none";
  location.reload();
});

closeRegModal.addEventListener("click", () => {
  registerModalMask.style.display = "none";
});

registerModalMask.addEventListener("click", (e) => {
  if (e.target === registerModalMask) {
    registerModalMask.style.display = "none";
  }
});

// 1. 搜索函数 (供按钮点击调用)
function searchMessages() {
  const keywordInput = document.getElementById("searchKeyword");
  currentQuery.keyword = keywordInput.value.trim();
  currentQuery.pageNum = 1; // 搜索时重置为第一页
  fetchMessages();
}

// 2. 分页点击函数
function goToPage(page) {
  currentQuery.pageNum = page;
  fetchMessages();
}

// 3. 核心：发送请求并处理分页
function fetchMessages() {
  // 显示加载状态 (可选)
  document.getElementById("msgTableBody").innerHTML =
    '<tr><td colspan="7">加载中...</td></tr>';

  fetch("/getMsg", {
    method: "POST",
    headers: {
      "Content-Type": "application/json; charset=utf-8",
    },
    body: JSON.stringify({
      keyword: currentQuery.keyword,
      pageNum: currentQuery.pageNum,
      pageSize: currentQuery.pageSize,
    }),
  })
    .then((response) => response.json())
    .then((result) => {
      if (result.code === 0) {
        // 4. 渲染表格数据
        renderMessages(result.data.list);

        // 5. 渲染分页控件
        renderPagination(result.data);
      } else {
        showErrorMessage(result.msg);
      }
    })
    .catch((error) => {
      console.error("Error:", error);
      document.getElementById("msgTableBody").innerHTML =
        '<tr><td colspan="7">请求失败，请重试</td></tr>';
    });
}

// 6. 渲染表格数据
function renderMessages(messages) {
  const tableBody = document.getElementById("msgTableBody");
  tableBody.innerHTML = "";

  if (messages.length === 0) {
    tableBody.innerHTML = '<tr><td colspan="7">暂无数据</td></tr>';
    return;
  }

  messages.forEach((msg) => {
    const row = document.createElement("tr");
    row.innerHTML = `
            <td>${msg.msgId}</td>
            <td>${msg.msgContent}</td>
            <td>${msg.updateTime}</td>
            <td>${msg.createBy}</td>
            <td>${msg.groupId}</td>
            <td>${msg.msgType}</td>
        `;
    tableBody.appendChild(row);
  });
}

// 7. 渲染分页控件 (核心逻辑)
function renderPagination(pageInfo) {
  const paginationContainer = document.getElementById("pagination");
  let paginationHTML = "";

  const { pageNum, pages, isFirstPage, isLastPage } = pageInfo;

  // 上一页按钮
  if (!isFirstPage) {
    paginationHTML += `<button onclick="goToPage(${
      pageNum - 1
    })">&laquo; 上一页</button>`;
  } else {
    paginationHTML += `<span class="disabled">&laquo; 上一页</span>`;
  }

  // 页码按钮 (简单的逻辑：显示当前页前后各2页)
  const startPage = Math.max(1, pageNum - 2);
  const endPage = Math.min(pages, pageNum + 2);

  for (let i = startPage; i <= endPage; i++) {
    if (i === pageNum) {
      paginationHTML += `<strong> ${i} </strong>`; // 当前页高亮
    } else {
      paginationHTML += `<button onclick="goToPage(${i})">${i}</button>`;
    }
  }

  // 下一页按钮
  if (!isLastPage) {
    paginationHTML += `<button onclick="goToPage(${
      pageNum + 1
    })">下一页 &raquo;</button>`;
  } else {
    paginationHTML += `<span class="disabled">下一页 &raquo;</span>`;
  }

  paginationContainer.innerHTML = paginationHTML;
}

// 显示红色提示消息的函数
function showErrorMessage(msg) {
  // 1. 创建一个 div 元素作为提示框
  const msgBox = document.createElement("div");

  // 2. 设置样式（红色文字、居中、背景、位置等）
  msgBox.style.cssText = `
        position: fixed;
        top: 20px;
        left: 50%;
        transform: translateX(-50%);
        background-color: #ffebee; /* 浅红色背景 */
        color: #c62828;            /* 深红色文字 */
        padding: 10px 20px;
        border: 1px solid #ef9a9a;
        border-radius: 4px;
        font-size: 18px;
        z-index: 1000;
        box-shadow: 0 2px 5px rgba(0,0,0,0.2);
        transition: opacity 0.5s ease; /* 添加淡出效果 */
    `;

  // 3. 设置显示的内容
  msgBox.textContent = msg;

  // 4. 将提示框添加到页面 body 中
  document.body.appendChild(msgBox);

  // 5. 设置 2 秒后自动消失（淡出并移除）
  setTimeout(() => {
    msgBox.style.opacity = "0";
    setTimeout(() => {
      if (msgBox.parentNode) {
        document.body.removeChild(msgBox);
      }
    }, 500); // 等待淡出动画结束再移除
  }, 2000);
}

const logoutBtn = document.getElementById("logout");
function initUserMenu() {
  // 获取登录状态
  const isLogin =
    !!localStorage.getItem("currentUser") &&
    !!localStorage.getItem("userToken");

  // 1. 控制退出选项的显示/隐藏
  if (isLogin) {
    logoutBtn.style.display = "block"; // 登录状态显示退出
  } else {
    logoutBtn.style.display = "none"; // 非登录状态隐藏退出
  }
}

// 绑定退出按钮点击事件
logoutBtn.addEventListener("click", function (e) {
  e.preventDefault(); // 阻止a标签默认跳转
  // 确认退出
  if (confirm("确定要退出登录吗？")) {
    // 清空localStorage中的登录数据
    localStorage.removeItem("userToken");
    localStorage.removeItem("currentUser");
    localStorage.removeItem("currentUserId");
    // 刷新菜单状态
    initUserMenu();
    window.location.reload();
  }
});

/*我的空间*/
const spaceLink = document.getElementById("spaceLink");
// 绑定点击事件
spaceLink.addEventListener("click", async function (e) {
  e.preventDefault(); // 阻止a标签默认跳转

  // 校验登录状态：判断localStorage是否存在用户和token
  const currentUser = localStorage.getItem("currentUser");
  const userToken = localStorage.getItem("userToken");
  //判断userToken是否失效
  if ( !currentUser || !userToken || getTokenExpireTime(userToken)===0) {
    handleTokenExpire();
    return;
  }

  // 已登录直接跳转到myspace.html，不再携带接口数据
  window.open("myspace.html", "_blank"); // 新开标签页跳转
});

/**
 * 解析JWT令牌，获取payload中的过期时间
 * @param {string} token - 本地存储的JWT令牌
 * @returns {number} 过期时间戳（毫秒），解析失败返回0
 */
const getTokenExpireTime = (token) => {
  if (!token) return 0;
  try {
    // 分割JWT，获取payload部分（注意解码前需处理Base64URL编码）
    const payloadBase64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    // Base64解码（浏览器环境使用atob，Node环境需用Buffer）
    const payloadJson = decodeURIComponent(escape(window.atob(payloadBase64)));
    const payload = JSON.parse(payloadJson);
    // exp是秒级时间戳，转换为毫秒级（方便与Date.now()对比）
    return payload.exp * 1000;
  } catch (error) {
    console.error('JWT解析失败：', error);
    return 0;
  }
};

/**
 * @param {number} advanceTime - 提前刷新时间（默认5分钟，单位毫秒）
 */
const startTokenRefreshMonitor = (advanceTime = 5 * 60 * 1000) => {
  // 清除已有定时任务（避免重复创建）
  if (window.tokenRefreshTimer) clearInterval(window.tokenRefreshTimer);

  const refreshToken = async () => {
    const currentToken = localStorage.getItem('userToken');
    if (!currentToken) {
      clearInterval(window.tokenRefreshTimer);
      return;
    }

    const expireTime = getTokenExpireTime(currentToken);
    const now = Date.now();
    const remainingTime = expireTime - now;

    // 剩余时间小于提前刷新时间，且大于0（未过期），则刷新token
    if (remainingTime > 0 && remainingTime <= advanceTime) {
      try {
        // 调用后端刷新token接口（需后端提供refreshToken接口，可携带旧token或refresh_token）
        const response = await fetch('/refreshToken', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          }
        });

        const result = await response.json();
        if (result.code === 0 && result.data.newJwtToken) {
          // 更新localStorage中的token
          localStorage.setItem('userToken', result.data.newJwtToken);
        } else {
          // 刷新失败（如旧token已无效），清除token并跳转登录页
          handleTokenExpire();
        }
      } catch (error) {
        console.error('token刷新失败：', error);
        handleTokenExpire();
      }
    }

    // 若token已过期，直接处理过期逻辑
    if (remainingTime <= 0) {
      clearInterval(window.tokenRefreshTimer);
      handleTokenExpire();
    }
  };

  // 初始化时先执行一次检查，之后每隔1分钟检查一次（可调整检查频率）
  refreshToken();
  window.tokenRefreshTimer = setInterval(refreshToken, 30 * 60 * 1000);
};

/**
 * token过期统一处理
 */
const handleTokenExpire = () => {
  // 1. 清除localStorage中的JWT令牌
  localStorage.removeItem('userToken');
  // 2. 提示用户token过期
  alert('登录信息已过期，请重新登录');
  // 3. 跳转至登录页（根据你的项目路由调整）
  loginModalMask.style.display = "flex"; // 直接弹出登录框
};