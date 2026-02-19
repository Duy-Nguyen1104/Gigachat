import { useAuth } from "../../context/AuthContext";
import { MessageSquare, LogOut, User } from "lucide-react";

export default function Navbar() {
  const { user, logout } = useAuth();

  return (
    <div className="w-20 bg-gray-800 flex flex-col items-center py-6 gap-4">
      {/* Logo */}
      <div className="w-12 h-12 bg-brand-300 rounded-2xl flex items-center justify-center mb-2 shadow-lg">
        <MessageSquare className="w-6 h-6" strokeWidth={2.5} />
      </div>

      {/* Spacer */}
      <div className="flex-1" />

      {/* User Avatar */}
      <div className="w-12 h-12 rounded-2xl bg-brand-300 flex items-center justify-center overflow-hidden group hover:ring-2 hover:ring-brand-300 transition cursor-pointer">
        {user?.avatarUrl ? (
          <img
            src={user.avatarUrl}
            alt="Avatar"
            className="w-full h-full object-cover"
          />
        ) : (
          <User className="w-5 h-5" />
        )}
      </div>

      {/* Logout */}
      <button
        onClick={logout}
        className="w-12 h-12 rounded-2xl flex items-center justify-center text-white hover:text-black hover:bg-brand-300 transition"
        title="Sign out"
      >
        <LogOut className="w-5 h-5" />
      </button>
    </div>
  );
}
