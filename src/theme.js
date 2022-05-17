(function () {
    // TOC部分
    if (document.getElementsByClassName("toc").length > 0) {
        // 控制TOC打开或关闭
        if (!document.getElementById("tocspan")) {
            document.body.innerHTML = '<span class="tocspan" id="tocspan">&#9776;</span>' + document.body.innerHTML;
        }
        document.getElementById("tocspan").onclick = function() {
            let toc = document.getElementsByClassName("toc")[0];
            if (toc.style.visibility == "hidden" || window.getComputedStyle(toc).visibility == "hidden") {
                toc.style.visibility = "visible";
            }
            else {
                toc.style.visibility = "hidden";
            }
        }
        let timer = null;// 定时器
        function updateTOC() {
            // TOC加粗当前在看的位置：获取所有h1、h2、...，滚动停止时判断在可视区域上方离可视区域最近的元素，更改TOC上对应节点的样式
            var m_as = new Array();// toc里所有的a标签
            var m_hs = new Array();// a标签对应的h1、...、h6标签
            m_as = document.getElementsByClassName("toc")[0].getElementsByTagName("a");
            for (let i = 0; i < m_as.length; i++) {
                // h1、...、h6的ID是a的href去掉最前面的#
                let hs = document.getElementById(decodeURI(m_as[i].getAttribute("href")).substring(1));
                if (hs) {
                    m_hs[m_hs.length] = hs;
                }
            }
            if (m_hs.length == 0 || m_as.length == 0) {
                return;
            }
            // getBoundingClientRect().top 最接近0的负值就是当前在看的部分
            // 由于点击toc跳转时只能跳转到元素上方，如果元素有padding就会判定上一个元素是在看，所以还要再预留自身高度
            let tempElement = m_hs[0];
            let distance = m_hs[0].getBoundingClientRect().top - m_hs[0].clientHeight;
            for (let i = 0; i < m_hs.length; i++) {
                let tempT = m_hs[i].getBoundingClientRect().top - m_hs[i].clientHeight;
                if (distance > 0 || tempT < 0 && tempT > distance) {
                    tempElement = m_hs[i];
                    distance = tempT;
                }
            }
            // 要是全部的distance都大于零，那么就是在页面最上方还没有出现过hx标签的地方，这时要找最接近0的正值
            if (tempElement > 0) {
                distance = -1;
                for (let i = 0; i < m_hs.length; i++) {
                    let tempT = m_hs[i].getBoundingClientRect().top - m_hs[i].clientHeight;
                    if (tempT > 0 && distance == -1 || tempT < Math.abs(distance)) {
                        tempElement = m_hs[i];
                        distance = tempT;
                    }
                }
            }
            if (tempElement) {
                // 把所有TOC内的a标签变成普通状态，然后把当前在看的h1、...、h6标签对应的a标签改变样式
                for (let j = 0; j < m_as.length; j++) {
                    m_as[j].style.fontWeight = "";
                    m_as[j].style.color = "";
                    if (decodeURI(m_as[j].getAttribute("href")) == "#" + tempElement.getAttribute("id")) {
                        m_as[j].style.fontWeight = "bold";
                        m_as[j].style.color = "#000";
                    }
                }
            }
        }
        window.onscroll = function () {
            // 滚动停止再更新toc
            clearTimeout(timer);
            timer = setTimeout(updateTOC, 200);
        }
        // TOC内的a标签点击滚动到指定位置，而非跳转
        let toc_a = document.getElementsByClassName("toc")[0].getElementsByTagName("a");
        for (let i = 0; i < toc_a.length; i++) {
            // h1、...、h6的ID是a的href去掉最前面的#
            let title = document.getElementById(decodeURI(toc_a[i].getAttribute("href")).substring(1));
            if (title) {
                toc_a[i].onclick = function () {
                    title.scrollIntoView({
                        behavior: "smooth",
                        block: "start"
                    });
                    return false;
                }
            }
        }
    }
})()