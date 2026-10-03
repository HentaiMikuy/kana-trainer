package com.konomip.kanatrainer.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 边到边布局（edge-to-edge）的留白约定：
 *
 * - 顶部安全区（状态栏 / 刘海）交给各屏幕的 TopAppBar 自己吸收。AppBar 背景因此会一直画到
 *   屏幕顶边（状态栏图标之下），而不是从状态栏下方才开始绘制，顶部就不会先空出一段。
 * - 左、右、下在这里统一让开，避免手势条或刘海盖住内容。
 *
 * 用法：屏幕根容器写成 `modifier.edgeToEdgeRoot()`，TopAppBar 不再接受额外 padding。
 */
@Composable
fun Modifier.edgeToEdgeRoot(): Modifier = this.windowInsetsPadding(WindowInsets.safeDrawing)
