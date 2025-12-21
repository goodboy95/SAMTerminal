import { useEffect } from 'react';
import { Outlet, useNavigate, useLocation, Link } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { 
  LayoutDashboard, 
  Image as ImageIcon, 
  Map as MapIcon, 
  Users, 
  Settings, 
  Activity,
  LogOut 
} from 'lucide-react';
import { cn } from '@/lib/utils';

const AdminLayout = () => {
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    const isLoggedIn = localStorage.getItem('is_admin_logged_in');
    const token = localStorage.getItem('sam_token');
    if (!isLoggedIn || !token) {
      navigate('/admin');
    }
  }, [navigate]);

  const handleLogout = () => {
    localStorage.removeItem('is_admin_logged_in');
    navigate('/admin');
  };

  const navItems = [
    { path: '/admin/dashboard/firefly', label: '流萤立绘管理', icon: ImageIcon },
    { path: '/admin/dashboard/locations', label: '区域与星球', icon: MapIcon },
    { path: '/admin/dashboard/characters', label: '角色档案', icon: Users },
    { path: '/admin/dashboard/monitor', label: '用户 Token 监测', icon: Activity },
    { path: '/admin/dashboard/settings', label: 'LLM API 池', icon: Settings },
  ];

  return (
    <div className="min-h-screen bg-slate-950 flex">
      {/* Sidebar */}
      <div className="w-64 bg-slate-900 border-r border-white/10 flex flex-col">
        <div className="p-6 border-b border-white/10">
          <h1 className="text-xl font-bold text-white flex items-center gap-2">
            <LayoutDashboard className="text-teal-400" />
            S.A.M. Admin
          </h1>
        </div>
        
        <nav className="flex-1 p-4 space-y-2">
          {navItems.map((item) => (
            <Link key={item.path} to={item.path}>
              <Button
                variant="ghost"
                className={cn(
                  "w-full justify-start text-slate-400 hover:text-white hover:bg-white/5",
                  location.pathname === item.path && "bg-teal-500/10 text-teal-400 hover:bg-teal-500/20"
                )}
              >
                <item.icon className="mr-2 w-4 h-4" />
                {item.label}
              </Button>
            </Link>
          ))}
        </nav>

        <div className="p-4 border-t border-white/10">
          <Button 
            variant="destructive" 
            className="w-full justify-start"
            onClick={handleLogout}
          >
            <LogOut className="mr-2 w-4 h-4" />
            退出登录
          </Button>
        </div>
      </div>

      {/* Main Content */}
      <div className="flex-1 overflow-auto">
        <header className="h-16 border-b border-white/10 bg-slate-900/50 backdrop-blur flex items-center px-8 justify-between">
          <h2 className="text-white font-medium">
            {navItems.find(i => i.path === location.pathname)?.label || '控制台'}
          </h2>
          <div className="text-xs text-slate-500 font-mono">System Status: ONLINE</div>
        </header>
        <main className="p-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default AdminLayout;
