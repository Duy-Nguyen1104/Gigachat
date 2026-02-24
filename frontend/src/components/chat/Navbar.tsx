import { useState, useRef } from "react";
import { useAuth } from "../../context/AuthContext";
import {
  MessageSquare,
  LogOut,
  User as UserIcon,
  Settings,
  Camera,
  Loader2,
  X,
} from "lucide-react";
import { updateProfile, getAvatarUploadUrl } from "../../api/user";
import axios from "axios";

export default function Navbar() {
  const { user, logout, updateUser } = useAuth();
  const [showDropdown, setShowDropdown] = useState(false);
  const [showProfileModal, setShowProfileModal] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [isUpdating, setIsUpdating] = useState(false);

  const fileInputRef = useRef<HTMLInputElement>(null);

  const [displayName, setDisplayName] = useState(user?.displayName || "");
  const [email, setEmail] = useState(user?.email || "");

  const handleAvatarClick = () => {
    fileInputRef.current?.click();
  };

  const handleAvatarChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    try {
      setIsUploading(true);
      const { uploadUrl, viewUrl } = await getAvatarUploadUrl({
        fileName: file.name,
        fileType: file.type,
      });

      await axios.put(uploadUrl, file, {
        headers: {
          "Content-Type": file.type,
        },
      });

      // Update local state with the presigned GET URL returned by the server
      if (user) {
        updateUser({ ...user, avatarUrl: viewUrl });
      }
    } catch (err) {
      console.error("Avatar upload failed", err);
      alert("Failed to upload avatar. Please try again.");
    } finally {
      setIsUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  };

  const handleUpdateProfile = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setIsUpdating(true);
      const updatedUser = await updateProfile({ displayName, email });
      updateUser(updatedUser);
      setShowProfileModal(false);
    } catch (err) {
      console.error("Failed to update profile", err);
    } finally {
      setIsUpdating(false);
    }
  };

  return (
    <div className="w-20 bg-gray-900 flex flex-col items-center py-6 gap-4 relative z-50">
      {/* Logo */}
      <div className="w-12 h-12 bg-brand-400 rounded-2xl flex items-center justify-center mb-2 shadow-lg">
        <MessageSquare className="w-6 h-6 text-gray-900" strokeWidth={2.5} />
      </div>

      {/* Spacer */}
      <div className="flex-1" />

      {/* User Avatar & Dropdown */}
      <div className="relative">
        <button
          onClick={() => setShowDropdown(!showDropdown)}
          disabled={isUploading}
          className="w-12 h-12 rounded-2xl bg-gray-700 flex items-center justify-center overflow-hidden hover:ring-2 hover:ring-brand-400 transition cursor-pointer relative disabled:cursor-not-allowed"
        >
          {user?.avatarUrl ? (
            <img
              src={user.avatarUrl}
              alt="Avatar"
              className={`w-full h-full object-cover ${isUploading ? "opacity-40" : ""}`}
            />
          ) : (
            <UserIcon
              className={`w-5 h-5 text-gray-400 ${isUploading ? "opacity-40" : ""}`}
            />
          )}
          {isUploading && (
            <div className="absolute inset-0 flex items-center justify-center">
              <Loader2 className="w-5 h-5 text-brand-400 animate-spin" />
            </div>
          )}
        </button>

        {showDropdown && (
          <>
            <div
              className="fixed inset-0 z-40"
              onClick={() => setShowDropdown(false)}
            />
            <div className="absolute bottom-0 left-16 mb-2 w-48 bg-white rounded-2xl shadow-xl border border-gray-100 py-2 z-50 animate-in fade-in slide-in-from-left-2 duration-200">
              <div className="px-4 py-2 border-b border-gray-50">
                <p className="text-xs font-semibold text-gray-900 truncate">
                  {user?.displayName || user?.username}
                </p>
                <p className="text-[10px] text-gray-500 truncate">
                  {user?.email}
                </p>
              </div>
              <button
                onClick={() => {
                  setShowProfileModal(true);
                  setShowDropdown(false);
                }}
                className="w-full flex items-center gap-3 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 transition"
              >
                <Settings className="w-4 h-4" />
                <span>Settings</span>
              </button>
              <button
                onClick={() => {
                  handleAvatarClick();
                  setShowDropdown(false);
                }}
                className="w-full flex items-center gap-3 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50 transition"
              >
                <Camera className="w-4 h-4" />
                <span>Change Avatar</span>
              </button>
              <div className="h-px bg-gray-100 my-1" />
              <button
                onClick={logout}
                className="w-full flex items-center gap-3 px-4 py-2 text-sm text-red-600 hover:bg-red-50 transition"
              >
                <LogOut className="w-4 h-4" />
                <span>Logout</span>
              </button>
            </div>
          </>
        )}
      </div>

      <input
        type="file"
        ref={fileInputRef}
        onChange={handleAvatarChange}
        className="hidden"
        accept="image/*"
      />

      {/* Profile Modal */}
      {showProfileModal && (
        <div className="fixed inset-0 z-[60] flex items-center justify-center p-4 bg-black/40 backdrop-blur-sm">
          <div className="bg-white rounded-3xl shadow-2xl w-full max-w-md overflow-hidden animate-in zoom-in-95 duration-200">
            <div className="px-8 pt-8 pb-6">
              <div className="flex items-center justify-between mb-6">
                <h2 className="text-2xl font-bold text-gray-900">
                  Profile Settings
                </h2>
                <button
                  onClick={() => setShowProfileModal(false)}
                  className="p-2 rounded-xl hover:bg-gray-100 text-gray-400 transition"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              <form onSubmit={handleUpdateProfile} className="space-y-4">
                <div>
                  <label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2 ml-1">
                    Display Name
                  </label>
                  <input
                    type="text"
                    value={displayName}
                    onChange={(e) => setDisplayName(e.target.value)}
                    className="w-full px-4 py-3 rounded-2xl bg-gray-50 border border-gray-100 text-sm focus:outline-none focus:ring-2 focus:ring-brand-400 focus:border-transparent transition"
                    placeholder="Enter display name"
                  />
                </div>
                <div>
                  <label className="block text-xs font-semibold text-gray-500 uppercase tracking-wider mb-2 ml-1">
                    Email Address
                  </label>
                  <input
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="w-full px-4 py-3 rounded-2xl bg-gray-50 border border-gray-100 text-sm focus:outline-none focus:ring-2 focus:ring-brand-400 focus:border-transparent transition"
                    placeholder="Enter email"
                  />
                </div>
                <div className="pt-4">
                  <button
                    type="submit"
                    disabled={isUpdating}
                    className="w-full py-3.5 bg-brand-400 hover:bg-brand-500 text-gray-900 font-bold rounded-2xl shadow-lg shadow-brand-200 transition disabled:opacity-50 flex items-center justify-center gap-2"
                  >
                    {isUpdating ? (
                      <Loader2 className="w-5 h-5 animate-spin" />
                    ) : (
                      "Save Changes"
                    )}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
