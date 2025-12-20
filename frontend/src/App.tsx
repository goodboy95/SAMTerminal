import { Toaster } from "@/components/ui/toaster";
import { Toaster as Sonner } from "@/components/ui/sonner";
import { TooltipProvider } from "@/components/ui/tooltip";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import Index from "./pages/Index";
import GamePage from "./pages/GamePage";
import NotFound from "./pages/NotFound";

// Admin Pages
import AdminLogin from "./pages/admin/AdminLogin";
import AdminLayout from "./pages/admin/AdminLayout";
import FireflyArtManager from "./pages/admin/FireflyArtManager";
import LocationManager from "./pages/admin/LocationManager";
import CharacterManager from "./pages/admin/CharacterManager";
import LLMSettings from "./pages/admin/LLMSettings";
import UserMonitor from "./pages/admin/UserMonitor";

const queryClient = new QueryClient();

const App = () => (
  <QueryClientProvider client={queryClient}>
    <TooltipProvider>
      <Toaster />
      <Sonner />
      <BrowserRouter>
        <Routes>
          {/* Public Routes */}
          <Route path="/" element={<Index />} />
          <Route path="/game" element={<GamePage />} />

          {/* Admin Routes */}
          <Route path="/admin" element={<AdminLogin />} />
          <Route path="/admin/dashboard" element={<AdminLayout />}>
            <Route index element={<Navigate to="/admin/dashboard/firefly" replace />} />
            <Route path="firefly" element={<FireflyArtManager />} />
            <Route path="locations" element={<LocationManager />} />
            <Route path="characters" element={<CharacterManager />} />
            <Route path="monitor" element={<UserMonitor />} />
            <Route path="settings" element={<LLMSettings />} />
          </Route>

          {/* 404 */}
          <Route path="*" element={<NotFound />} />
        </Routes>
      </BrowserRouter>
    </TooltipProvider>
  </QueryClientProvider>
);

export default App;
