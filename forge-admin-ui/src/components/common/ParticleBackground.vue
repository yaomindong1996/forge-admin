<template>
  <div ref="containerRef" class="particle-background">
    <canvas ref="canvasRef" class="particle-canvas"></canvas>
    <div v-if="$slots.default" class="particle-content">
      <slot />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'

const props = defineProps({
  // 粒子数量
  particleCount: {
    type: Number,
    default: 100
  },
  // 粒子最小尺寸
  minSize: {
    type: Number,
    default: 1
  },
  // 粒子最大尺寸
  maxSize: {
    type: Number,
    default: 3
  },
  // 粒子颜色
  color: {
    type: String,
    default: '#3b82f6'
  },
  // 连线距离
  connectionDistance: {
    type: Number,
    default: 120
  },
  // 粒子速度
  speed: {
    type: Number,
    default: 0.5
  },
  // 是否显示连线
  showConnections: {
    type: Boolean,
    default: true
  },
  // 鼠标交互
  mouseInteraction: {
    type: Boolean,
    default: true
  },
  // 鼠标影响范围
  mouseRadius: {
    type: Number,
    default: 150
  },
  // 粒子透明度
  opacity: {
    type: Number,
    default: 0.5
  }
})

const containerRef = ref(null)
const canvasRef = ref(null)
let ctx = null
let animationId = null
let particles = []
let mouse = { x: null, y: null }

class Particle {
  constructor(canvas) {
    this.canvas = canvas
    this.x = Math.random() * canvas.width
    this.y = Math.random() * canvas.height
    this.size = Math.random() * (props.maxSize - props.minSize) + props.minSize
    this.speedX = (Math.random() - 0.5) * props.speed * 2
    this.speedY = (Math.random() - 0.5) * props.speed * 2
    this.baseX = this.x
    this.baseY = this.y
    this.density = (Math.random() * 30) + 1
  }

  update() {
    // 鼠标交互
    if (props.mouseInteraction && mouse.x != null) {
      let dx = mouse.x - this.x
      let dy = mouse.y - this.y
      let distance = Math.sqrt(dx * dx + dy * dy)
      let forceDirectionX = dx / distance
      let forceDirectionY = dy / distance
      let maxDistance = props.mouseRadius
      let force = (maxDistance - distance) / maxDistance
      let directionX = forceDirectionX * force * this.density
      let directionY = forceDirectionY * force * this.density

      if (distance < maxDistance) {
        this.x -= directionX
        this.y -= directionY
      }
    }

    // 移动粒子
    this.x += this.speedX
    this.y += this.speedY

    // 边界检测
    if (this.x > this.canvas.width || this.x < 0) {
      this.speedX = -this.speedX
    }
    if (this.y > this.canvas.height || this.y < 0) {
      this.speedY = -this.speedY
    }
  }

  draw() {
    ctx.beginPath()
    ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2)
    ctx.closePath()
    ctx.fillStyle = props.color
    ctx.globalAlpha = props.opacity
    ctx.fill()
  }
}

// 初始化粒子
function initParticles() {
  if (!canvasRef.value) return
  
  const canvas = canvasRef.value
  canvas.width = containerRef.value.offsetWidth
  canvas.height = containerRef.value.offsetHeight
  
  ctx = canvas.getContext('2d')
  particles = []
  
  for (let i = 0; i < props.particleCount; i++) {
    particles.push(new Particle(canvas))
  }
}

// 绘制连线
function drawConnections() {
  if (!props.showConnections) return
  
  for (let a = 0; a < particles.length; a++) {
    for (let b = a; b < particles.length; b++) {
      let dx = particles[a].x - particles[b].x
      let dy = particles[a].y - particles[b].y
      let distance = Math.sqrt(dx * dx + dy * dy)
      
      if (distance < props.connectionDistance) {
        ctx.beginPath()
        ctx.strokeStyle = props.color
        ctx.globalAlpha = 1 - (distance / props.connectionDistance) * props.opacity
        ctx.lineWidth = 1
        ctx.moveTo(particles[a].x, particles[a].y)
        ctx.lineTo(particles[b].x, particles[b].y)
        ctx.stroke()
        ctx.closePath()
      }
    }
  }
}

// 动画循环
function animate() {
  if (!canvasRef.value || !ctx) return
  
  ctx.clearRect(0, 0, canvasRef.value.width, canvasRef.value.height)
  
  // 更新和绘制粒子
  particles.forEach(particle => {
    particle.update()
    particle.draw()
  })
  
  // 绘制连线
  drawConnections()
  
  animationId = requestAnimationFrame(animate)
}

// 调整画布大小
function handleResize() {
  if (canvasRef.value && containerRef.value) {
    canvasRef.value.width = containerRef.value.offsetWidth
    canvasRef.value.height = containerRef.value.offsetHeight
    initParticles()
  }
}

// 鼠标移动处理
function handleMouseMove(e) {
  if (!containerRef.value) return
  const rect = containerRef.value.getBoundingClientRect()
  mouse.x = e.clientX - rect.left
  mouse.y = e.clientY - rect.top
}

function handleMouseLeave() {
  mouse.x = null
  mouse.y = null
}

onMounted(() => {
  initParticles()
  animate()
  
  window.addEventListener('resize', handleResize)
  
  if (containerRef.value) {
    containerRef.value.addEventListener('mousemove', handleMouseMove)
    containerRef.value.addEventListener('mouseleave', handleMouseLeave)
  }
})

onUnmounted(() => {
  if (animationId) {
    cancelAnimationFrame(animationId)
  }
  
  window.removeEventListener('resize', handleResize)
  
  if (containerRef.value) {
    containerRef.value.removeEventListener('mousemove', handleMouseMove)
    containerRef.value.removeEventListener('mouseleave', handleMouseLeave)
  }
})

// 监听属性变化
watch(() => props.particleCount, () => {
  initParticles()
})
</script>

<style scoped>
.particle-background {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.particle-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.particle-content {
  position: relative;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 1;
}

.particle-content > * {
  pointer-events: auto;
}
</style>