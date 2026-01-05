// 全局变量，用于存储从接口获取的根数据
let rootData = null;

// 页面加载完成后执行
document.addEventListener("DOMContentLoaded", function () {
  fetchData();
});

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

// 3. 加载并显示图片 (核心修改部分)
function loadFolder(folderPath) {
  const contentArea = document.getElementById("contentArea");
  contentArea.innerHTML = "<p>加载中...</p>";

  // 准备 POST 请求的数据
  const postData = {
    path: folderPath,
  };

  fetch("/listImages", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(postData),
  })
    .then((response) => response.json())
    .then((imgData) => {
      if (imgData.code === 0 && Array.isArray(imgData.data)) {
        renderImageGrid(imgData.data);
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

  // 2. 生成 HTML
  // 使用 CSS Grid 进行平铺布局
  let html = `<div class="image-grid">`;
  webPathList.forEach((src) => {
    html += `
            <div class="image-item">
                <img src="${src}" onclick="openImage('${src}')">
            </div>
        `;
  });
  html += `</div>`;

  contentArea.innerHTML = html;
}

// 5. 可选：点击图片放大
function openImage(src) {
  window.open(src, "_blank");
}

// 1. 获取DOM元素
const userIcon = document.getElementById("userIcon");
const userMenu = document.getElementById("userMenu");

// 2. 给用户图标添加点击事件（切换菜单显示/隐藏）
userIcon.addEventListener("click", function (e) {
  // 阻止事件冒泡（避免触发document的点击事件，导致菜单刚显示就隐藏）
  e.stopPropagation();
  // 切换active类，控制菜单显示/隐藏
  userMenu.classList.toggle("active");
});

// 3. 给document添加点击事件（点击其他地方隐藏菜单）
document.addEventListener("click", function () {
  // 移除active类，隐藏菜单
  userMenu.classList.remove("active");
});

// 4. 给菜单本身添加点击事件（阻止事件冒泡，避免点击菜单项时菜单隐藏）
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

// 打开登录弹窗（点击登录链接）
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

loginModalMask.addEventListener("click", (e) => {
  // 点击遮罩层（非弹窗内容）关闭弹窗
  if (e.target === loginModalMask) {
    loginModalMask.style.display = "none";
  }
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
    const response = await fetch("http://127.0.0.1:8081/login", {
      method: "POST",
      headers: {
        "Content-Type": "application/json", // 与postman一致
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
    const token = result.token; // 提取Token（假设后端返回格式：{ "token": "xxx.yyy.zzz" }）

    // （4）存储Token（两种常用方式，按需选择）
    // 方式1：localStorage - 持久化存储（关闭浏览器后仍存在，需手动清除）
    localStorage.setItem("userToken", token);
    // 方式2：sessionStorage - 会话级存储（关闭浏览器/标签页后消失，更安全）
    // sessionStorage.setItem('userToken', token);

    alert("登录成功！Token已存储");
    loginModalMask.style.display = "none"; // 关闭弹窗
    // 关键：登录成功后调用视图更新函数
    updateUserView(username);
  } catch (error) {
    console.error("登录异常：", error);
    alert("登录失败：" + error.message);
  }
});

/*更新用户视图*/
function updateUserView(username) {
  // 4.1 修改userIcon的title为当前用户名（鼠标悬浮显示）
  userIcon.setAttribute("title", username);

  // 4.2 隐藏「注册」选项（两种方式可选，推荐方式1）
  // 方式1：直接隐藏<li>标签，彻底不显示
  registerLink.parentElement.style.display = "none";
  // 方式2：移除整个注册<li>标签（不可逆，如需恢复需重新创建）
  // registerLink.parentElement.remove();

  // 4.3 隐藏「登录」选项（登录后无需再显示登录）
  loginLink.parentElement.style.display = "none";
}

/*页面初始化：判断是否已登录，恢复视图状态（避免刷新页面后还原）*/
window.onload = function () {
  const currentUser = localStorage.getItem("currentUser");
  const userToken = localStorage.getItem("userToken");

  // 若存在有效Token和用户名，自动更新视图
  if (currentUser && userToken) {
    updateUserView(currentUser);
  }
};
