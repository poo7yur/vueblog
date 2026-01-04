// 全局变量，用于存储从接口获取的根数据
let rootData = null;

// 页面加载完成后执行
document.addEventListener('DOMContentLoaded', function () {
    fetchData();
});

// 1. 获取目录树数据
function fetchData() {
    //fetch('http://127.0.0.1:8080/scanner')
    fetch('/scanner')
        .then(response => response.json())
        .then(data => {
            if (data.code === 0) {
                rootData = data.data;
                renderTree(rootData, document.getElementById('treeContainer'));
            }
        })
        .catch(err => {
            document.getElementById('contentArea').innerHTML = `<p style="color:red">接口请求失败: ${err}</p>`;
        });
}

// 2. 递归渲染左侧树状菜单
function renderTree(node, container) {
    const ul = document.createElement('ul');
    ul.className = 'tree';

    node.children.forEach(child => {
        const li = document.createElement('li');

        if (child.children && child.children.length > 0) {
            // 父节点 (有子文件夹)
            const span = document.createElement('span');
            span.className = 'toggle';
            span.textContent = `${child.name} ▼`;
            li.appendChild(span);

            const subMenu = document.createElement('div');
            renderTree(child, subMenu);
            subMenu.style.display = 'none';
            li.appendChild(subMenu);

            span.addEventListener('click', function (e) {
                e.stopPropagation();
                const isBlock = subMenu.style.display === 'block';
                subMenu.style.display = isBlock ? 'none' : 'block';
                span.textContent = `${child.name} ${isBlock ? '▼' : '▲'}`;
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
// folderPath: 例如 "C:\\Users\\Admin\\Pictures\\save\\A\\A02"
function loadFolder(folderPath) {
    const contentArea = document.getElementById('contentArea');
    contentArea.innerHTML = '<p>加载中...</p>';

    // 准备 POST 请求的数据
    const postData = {
        path: folderPath
    };

    //fetch('http://127.0.0.1:8080/listImages', {
    fetch('/listImages', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(postData)
    })
    .then(response => response.json())
    .then(imgData => {
        if (imgData.code === 0 && Array.isArray(imgData.data)) {
            renderImageGrid(imgData.data);
        } else {
            contentArea.innerHTML = '<p>该文件夹为空或发生错误。</p>';
        }
    })
    .catch(err => {
        contentArea.innerHTML = `<p style="color:red">图片加载失败: ${err}</p>`;
    });
}

// 4. 渲染图片平铺列表
// filePathList: 接口返回的全路径数组
function renderImageGrid(filePathList) {
    const contentArea = document.getElementById('contentArea');

    // 1. 转换路径：将磁盘全路径转换为 Web 访问路径
    // 原始路径: C:\Users\Admin\Pictures\save\A\A02\image_2.jpg
    // 目标路径: /static/A/A02/image_2.jpg
    const baseUrl = '/static/';
    const webPathList = filePathList.map(fullPath => {
        // 将 Windows 路径分隔符 \ 替换为 / (防止路径显示错误)
        let webPath = fullPath.replace(/\\/g, '/');

        // 关键步骤：截取 "save" 目录之后的部分，并拼接到 baseUrl 后面
        // 假设后端映射的是 save 目录，所以我们需要去掉 "C:/Users/Admin/Pictures/save"
        const saveIndex = webPath.indexOf('/save/');

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
    webPathList.forEach(src => {
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
    window.open(src, '_blank');
}