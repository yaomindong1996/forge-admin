export function composeDocument(config, manifest) {
  const services = {}
  if (manifest.targets.includes('server')) {
    services.server = service(config.serverImage, manifest)
    services.server.entrypoint = ['java', '-jar', '/opt/forge/admin.jar',
      '--spring.config.additional-location=file:/run/forge/application.yml']
    services.server.ports = [port(config.serverPort, 8580)]
    services.server.volumes = [bind('./artifacts/backend/admin.jar', '/opt/forge/admin.jar'),
      bind(`${config.configRoot}/application.yml`, '/run/forge/application.yml')]
    services.server.working_dir = '/tmp'
  }
  if (manifest.targets.includes('ui')) {
    services.ui = service(config.uiImage, manifest)
    services.ui.ports = [port(config.uiPort, 8080)]
    services.ui.volumes = [bind('./artifacts/frontend', '/usr/share/nginx/html'),
      bind(`${config.configRoot}/nginx.conf`, '/etc/nginx/conf.d/default.conf')]
  }
  return { name: config.projectName, services }
}

function bind(source, target) {
  return { type: 'bind', source, target, read_only: true, bind: { create_host_path: false } }
}

function port(published, target) {
  return { target, published: String(published), host_ip: '127.0.0.1', protocol: 'tcp' }
}

function service(image, manifest) {
  return { image, pull_policy: 'never', read_only: true, restart: 'no',
    // 只用于 rootless Docker：容器 0 映射为宿主普通用户，才能读取私有只读挂载。
    // 本工具不运行 Docker；真正执行器必须先验证 rootless，不得在 rootful 环境直接复用。
    user: '0:0', cap_drop: ['ALL'], security_opt: ['no-new-privileges:true'],
    pids_limit: 256, mem_limit: '2048m', cpus: 2,
    tmpfs: ['/tmp:rw,noexec,nosuid,size=256m'],
    labels: { 'forge.release-id': `rel-${manifest.digest}`, 'forge.plugin-id': manifest.plugin.id,
      'forge.plugin-version': manifest.plugin.version } }
}
